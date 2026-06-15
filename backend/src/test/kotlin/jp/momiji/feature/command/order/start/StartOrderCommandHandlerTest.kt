package jp.momiji.feature.command.order.start

import jp.momiji.MomijiIntegrationTestBase
import jp.momiji.event.order.OrderStartedEvent
import jp.momiji.event.product.ProductCreatedEvent
import jp.momiji.event.product.ProductDiscontinuedEvent
import jp.momiji.event.stock.StockReceivedEvent
import jp.momiji.event.stock.StockReservedEvent
import jp.momiji.event.user.ShippingAddressRegisteredEvent
import org.junit.jupiter.api.Test

/**
 * 注文開始の整合境界（order_id ＋ user_id ＋ 全 product_id）を検証する。
 *
 * EventStore は全テストで共有・リセットされないため、 テストごとにユニークな id を使う。
 */
class StartOrderCommandHandlerTest : MomijiIntegrationTestBase() {
    private val brandId = "01HXYZBRAND000000000START01"

    private fun productCreated(
        productId: String,
        price: Int = 1000,
        name: String = "テスト商品",
        imageUrl: String? = null,
    ) = ProductCreatedEvent(
        id = productId,
        brandId = brandId,
        name = name,
        description = "説明",
        imageUrl = imageUrl,
        price = price,
    )

    private fun stockReceived(
        productId: String,
        onHand: Int,
    ) = StockReceivedEvent(productId = productId, receivedQuantity = onHand, onHandQuantity = onHand)

    private fun shippingRegistered(
        userId: String,
        addressId: String,
    ) = ShippingAddressRegisteredEvent(
        userId = userId,
        shippingAddressId = addressId,
        name = "山田太郎",
        phoneNumber = "090-1234-5678",
        postalCode = "1000001",
        prefecture = "東京都",
        city = "千代田区",
        streetAddress = "1-1-1",
        building = "",
        deliveryNote = "",
    )

    // shippingRegistered と一致する、 OrderStarted に焼き込まれる配送先スナップショット。
    private val expectedSnapshot =
        OrderStartedEvent.SnapshotShippingAddress(
            name = "山田太郎",
            phoneNumber = "090-1234-5678",
            postalCode = "1000001",
            prefecture = "東京都",
            city = "千代田区",
            streetAddress = "1-1-1",
            building = "",
            deliveryNote = "",
        )

    private fun command(
        orderId: String,
        userId: String,
        addressId: String,
        expectedTotalAmount: Long,
        items: List<StartOrderCommand.Item>,
    ) = StartOrderCommand(
        id = orderId,
        userId = userId,
        shippingAddressId = addressId,
        expectedTotalAmount = expectedTotalAmount,
        items = items,
    )

    @Test
    fun `正常系_在庫を確保して OrderStarted と StockReserved を発行する`() {
        val orderId = "01HXYZORDER000000000START01"
        val userId = "01HXYZUSER0000000000START01"
        val addressId = "01HXYZADDR0000000000START01"
        val p1 = "01HXYZPRODUCT00000000STAR011"

        fixture
            .given()
            .events(
                shippingRegistered(userId, addressId),
                // 商品に画像 URL あり → 注文明細にスナップショットされる。
                productCreated(p1, price = 1000, imageUrl = "https://img.example.com/p1.png"),
                stockReceived(p1, onHand = 10),
            ).`when`()
            .command(command(orderId, userId, addressId, expectedTotalAmount = 2000, items = listOf(StartOrderCommand.Item(p1, 2))))
            .then()
            .resultMessagePayload(StartOrderCommandResult.success())
            .events(
                OrderStartedEvent(
                    orderId = orderId,
                    userId = userId,
                    shippingAddress = expectedSnapshot,
                    items = listOf(OrderStartedEvent.SnapshotItem(p1, "テスト商品", 1000, 2, "https://img.example.com/p1.png")),
                ),
                StockReservedEvent(productId = p1, orderId = orderId, quantity = 2, reservedQuantity = 2),
            )
    }

    @Test
    fun `正常系_複数商品をまとめて予約する（全商品 atomic）`() {
        val orderId = "01HXYZORDER000000000START02"
        val userId = "01HXYZUSER0000000000START02"
        val addressId = "01HXYZADDR0000000000START02"
        val p1 = "01HXYZPRODUCT00000000STAR021"
        val p2 = "01HXYZPRODUCT00000000STAR022"

        fixture
            .given()
            .events(
                shippingRegistered(userId, addressId),
                // p1 は画像あり、 p2 は画像なし（null）。 両方が明細にそのままスナップショットされる。
                productCreated(p1, price = 1000, name = "商品A", imageUrl = "https://img.example.com/a.png"),
                stockReceived(p1, onHand = 10),
                productCreated(p2, price = 500, name = "商品B"),
                stockReceived(p2, onHand = 5),
            ).`when`()
            .command(
                command(
                    orderId,
                    userId,
                    addressId,
                    // 1000*2 + 500*3
                    expectedTotalAmount = 3500,
                    items = listOf(StartOrderCommand.Item(p1, 2), StartOrderCommand.Item(p2, 3)),
                ),
            ).then()
            .resultMessagePayload(StartOrderCommandResult.success())
            .events(
                OrderStartedEvent(
                    orderId = orderId,
                    userId = userId,
                    shippingAddress = expectedSnapshot,
                    items =
                        listOf(
                            OrderStartedEvent.SnapshotItem(p1, "商品A", 1000, 2, "https://img.example.com/a.png"),
                            // 画像なしは null のままスナップショット。
                            OrderStartedEvent.SnapshotItem(p2, "商品B", 500, 3, null),
                        ),
                ),
                StockReservedEvent(productId = p1, orderId = orderId, quantity = 2, reservedQuantity = 2),
                StockReservedEvent(productId = p2, orderId = orderId, quantity = 3, reservedQuantity = 3),
            )
    }

    @Test
    fun `既存予約があっても reservedQuantity を加算して予約する`() {
        val orderId = "01HXYZORDER000000000START03"
        val userId = "01HXYZUSER0000000000START03"
        val addressId = "01HXYZADDR0000000000START03"
        val p1 = "01HXYZPRODUCT00000000STAR031"
        val otherOrderId = "01HXYZORDER0000000START03OT"

        fixture
            .given()
            .events(
                shippingRegistered(userId, addressId),
                productCreated(p1, price = 1000),
                stockReceived(p1, onHand = 10),
                // 別注文が 3 個予約済み（available = 10 - 3 = 7）。
                StockReservedEvent(productId = p1, orderId = otherOrderId, quantity = 3, reservedQuantity = 3),
            ).`when`()
            .command(command(orderId, userId, addressId, expectedTotalAmount = 2000, items = listOf(StartOrderCommand.Item(p1, 2))))
            .then()
            .resultMessagePayload(StartOrderCommandResult.success())
            .events(
                OrderStartedEvent(
                    orderId = orderId,
                    userId = userId,
                    shippingAddress = expectedSnapshot,
                    items = listOf(OrderStartedEvent.SnapshotItem(p1, "テスト商品", 1000, 2)),
                ),
                // 3（既存）＋ 2（今回）= 5。
                StockReservedEvent(productId = p1, orderId = orderId, quantity = 2, reservedQuantity = 5),
            )
    }

    @Test
    fun `商品が空なら emptyOrder で何も発行しない`() {
        val orderId = "01HXYZORDER000000000START04"
        val userId = "01HXYZUSER0000000000START04"
        val addressId = "01HXYZADDR0000000000START04"

        fixture
            .given()
            .events(shippingRegistered(userId, addressId))
            .`when`()
            .command(command(orderId, userId, addressId, expectedTotalAmount = 0, items = emptyList()))
            .then()
            .resultMessagePayload(StartOrderCommandResult.emptyOrder())
            .noEvents()
    }

    @Test
    fun `他人の配送先IDは見えず shippingAddressNotFound（IDOR防止）`() {
        val orderId = "01HXYZORDER000000000START05"
        val attacker = "01HXYZUSER00000000START05AT"
        val victim = "01HXYZUSER00000000START05VI"
        val addressId = "01HXYZADDR0000000000START05"
        val p1 = "01HXYZPRODUCT00000000STAR051"

        fixture
            .given()
            .events(
                // 配送先は victim の所有（user_id 境界の外なので attacker の State には現れない）。
                shippingRegistered(victim, addressId),
                productCreated(p1, price = 1000),
                stockReceived(p1, onHand = 10),
            ).`when`()
            .command(command(orderId, attacker, addressId, expectedTotalAmount = 1000, items = listOf(StartOrderCommand.Item(p1, 1))))
            .then()
            .resultMessagePayload(StartOrderCommandResult.shippingAddressNotFound())
            .noEvents()
    }

    @Test
    fun `販売停止商品は productNotFound で何も発行しない`() {
        val orderId = "01HXYZORDER000000000START06"
        val userId = "01HXYZUSER0000000000START06"
        val addressId = "01HXYZADDR0000000000START06"
        val p1 = "01HXYZPRODUCT00000000STAR061"

        fixture
            .given()
            .events(
                shippingRegistered(userId, addressId),
                productCreated(p1, price = 1000),
                stockReceived(p1, onHand = 10),
                ProductDiscontinuedEvent(id = p1),
            ).`when`()
            .command(command(orderId, userId, addressId, expectedTotalAmount = 1000, items = listOf(StartOrderCommand.Item(p1, 1))))
            .then()
            .resultMessagePayload(StartOrderCommandResult.productNotFound(listOf(p1)))
            .noEvents()
    }

    @Test
    fun `合計金額が一致しないと amountMismatch で弾く`() {
        val orderId = "01HXYZORDER000000000START07"
        val userId = "01HXYZUSER0000000000START07"
        val addressId = "01HXYZADDR0000000000START07"
        val p1 = "01HXYZPRODUCT00000000STAR071"

        fixture
            .given()
            .events(
                shippingRegistered(userId, addressId),
                productCreated(p1, price = 1000),
                stockReceived(p1, onHand = 10),
            ).`when`()
            // server 権威価格は 1000*2=2000 だが client は 1500 を提示。
            .command(command(orderId, userId, addressId, expectedTotalAmount = 1500, items = listOf(StartOrderCommand.Item(p1, 2))))
            .then()
            .resultMessagePayload(StartOrderCommandResult.amountMismatch())
            .noEvents()
    }

    @Test
    fun `在庫不足なら outOfStock で何も発行しない`() {
        val orderId = "01HXYZORDER000000000START08"
        val userId = "01HXYZUSER0000000000START08"
        val addressId = "01HXYZADDR0000000000START08"
        val p1 = "01HXYZPRODUCT00000000STAR081"

        fixture
            .given()
            .events(
                shippingRegistered(userId, addressId),
                productCreated(p1, price = 1000),
                // 在庫は 1 個だけ。
                stockReceived(p1, onHand = 1),
            ).`when`()
            .command(command(orderId, userId, addressId, expectedTotalAmount = 2000, items = listOf(StartOrderCommand.Item(p1, 2))))
            .then()
            .resultMessagePayload(StartOrderCommandResult.outOfStock(listOf(p1)))
            .noEvents()
    }

    @Test
    fun `冪等性_既に開始済みの注文は新規イベントを出さず成功`() {
        val orderId = "01HXYZORDER000000000START09"
        val userId = "01HXYZUSER0000000000START09"
        val addressId = "01HXYZADDR0000000000START09"
        val p1 = "01HXYZPRODUCT00000000STAR091"

        fixture
            .given()
            .events(
                shippingRegistered(userId, addressId),
                productCreated(p1, price = 1000),
                stockReceived(p1, onHand = 10),
                StockReservedEvent(productId = p1, orderId = orderId, quantity = 2, reservedQuantity = 2),
                OrderStartedEvent(
                    orderId = orderId,
                    userId = userId,
                    shippingAddress = expectedSnapshot,
                    items = listOf(OrderStartedEvent.SnapshotItem(p1, "テスト商品", 1000, 2)),
                ),
            ).`when`()
            .command(command(orderId, userId, addressId, expectedTotalAmount = 2000, items = listOf(StartOrderCommand.Item(p1, 2))))
            .then()
            .resultMessagePayload(StartOrderCommandResult.success())
            .noEvents()
    }
}
