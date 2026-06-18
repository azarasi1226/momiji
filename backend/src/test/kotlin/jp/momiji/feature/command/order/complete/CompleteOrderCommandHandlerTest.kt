package jp.momiji.feature.command.order.complete

import jp.momiji.MomijiIntegrationTestBase
import jp.momiji.event.order.OrderCompletedEvent
import jp.momiji.event.order.OrderPaidEvent
import jp.momiji.event.order.OrderPaymentPreparedEvent
import jp.momiji.event.order.OrderShippedEvent
import jp.momiji.event.order.OrderStartedEvent
import jp.momiji.event.product.ProductCreatedEvent
import jp.momiji.event.stock.StockReceivedEvent
import jp.momiji.event.stock.StockReservationCommittedEvent
import jp.momiji.event.stock.StockReservedEvent
import org.junit.jupiter.api.Test

/**
 * 注文完了（SHIPPED → COMPLETED）と、 完了時の在庫引き当て確定（onHand・reserved を減らす）・冪等・ガードを検証する。
 * 整合境界は order_id（[jp.momiji.feature.command.order.OrderState]）＋ product_id（ProductsState）。
 *
 * EventStore は全テストで共有・リセットされないため、 テストごとにユニークな id を使う。
 */
class CompleteOrderCommandHandlerTest : MomijiIntegrationTestBase() {
    private fun productCreated(productId: String) =
        ProductCreatedEvent(
            id = productId,
            brandId = "01HXYZBRAND0000000000COMP1",
            name = "テスト商品",
            description = "説明",
            imageUrl = null,
            price = 1000,
        )

    private fun stockReceived(
        productId: String,
        onHand: Int,
    ) = StockReceivedEvent(productId = productId, receivedQuantity = onHand, onHandQuantity = onHand)

    private fun orderStarted(
        orderId: String,
        userId: String,
        productId: String,
        quantity: Int,
    ) = OrderStartedEvent(
        orderId = orderId,
        userId = userId,
        shippingAddress =
            OrderStartedEvent.SnapshotShippingAddress(
                name = "山田太郎",
                phoneNumber = "090-1234-5678",
                postalCode = "1000001",
                prefecture = "東京都",
                city = "千代田区",
                streetAddress = "1-1",
                building = "",
                deliveryNote = "",
            ),
        items = listOf(OrderStartedEvent.SnapshotItem(productId, "テスト商品", 1000, quantity)),
    )

    private fun paymentPrepared(orderId: String) =
        OrderPaymentPreparedEvent(orderId = orderId, paymentMethodId = "pm_COMP", paymentIntentId = "pi_COMP")

    private fun command(
        orderId: String,
        productIds: List<String>,
    ) = CompleteOrderCommand(orderId = orderId, productIds = productIds)

    @Test
    fun `SHIPPED の注文を COMPLETED にし、 OrderCompleted と在庫引き当て確定を発行する`() {
        val orderId = "01HXYZORDER000000000COMP01"
        val userId = "01HXYZUSER0000000000COMP01"
        val p1 = "01HXYZPRODUCT00000000COMP011"

        fixture
            .given()
            .events(
                productCreated(p1),
                stockReceived(p1, onHand = 10),
                StockReservedEvent(productId = p1, orderId = orderId, quantity = 2, reservedQuantity = 2),
                orderStarted(orderId, userId, p1, quantity = 2),
                paymentPrepared(orderId),
                OrderPaidEvent(orderId = orderId),
                OrderShippedEvent(orderId = orderId),
            ).`when`()
            .command(command(orderId, listOf(p1)))
            .then()
            .resultMessagePayload(CompleteOrderCommandResult.success())
            .events(
                OrderCompletedEvent(orderId = orderId),
                // 出荷確定: onHand 10→8、 reserved 2→0（available は 8 のまま）。
                StockReservationCommittedEvent(
                    productId = p1,
                    orderId = orderId,
                    quantity = 2,
                    onHandQuantity = 8,
                    reservedQuantity = 0,
                ),
            )
    }

    @Test
    fun `冪等性_既に COMPLETED なら no-op 成功（在庫を二重確定しない）`() {
        val orderId = "01HXYZORDER000000000COMP02"
        val userId = "01HXYZUSER0000000000COMP02"
        val p1 = "01HXYZPRODUCT00000000COMP021"

        fixture
            .given()
            .events(
                productCreated(p1),
                stockReceived(p1, onHand = 10),
                StockReservedEvent(productId = p1, orderId = orderId, quantity = 2, reservedQuantity = 2),
                orderStarted(orderId, userId, p1, quantity = 2),
                paymentPrepared(orderId),
                OrderPaidEvent(orderId = orderId),
                OrderShippedEvent(orderId = orderId),
                OrderCompletedEvent(orderId = orderId),
            ).`when`()
            .command(command(orderId, listOf(p1)))
            .then()
            .resultMessagePayload(CompleteOrderCommandResult.success())
            .noEvents()
    }

    @Test
    fun `未発送（PAID）の注文は完了できず cannotComplete`() {
        val orderId = "01HXYZORDER000000000COMP03"
        val userId = "01HXYZUSER0000000000COMP03"
        val p1 = "01HXYZPRODUCT00000000COMP031"

        fixture
            .given()
            .events(
                productCreated(p1),
                stockReceived(p1, onHand = 10),
                StockReservedEvent(productId = p1, orderId = orderId, quantity = 2, reservedQuantity = 2),
                orderStarted(orderId, userId, p1, quantity = 2),
                paymentPrepared(orderId),
                OrderPaidEvent(orderId = orderId),
            ).`when`()
            .command(command(orderId, listOf(p1)))
            .then()
            .resultMessagePayload(CompleteOrderCommandResult.cannotComplete())
            .noEvents()
    }

    @Test
    fun `存在しない注文への完了は cannotComplete`() {
        val orderId = "01HXYZORDER000000000COMP04"

        fixture
            .given()
            .noPriorActivity()
            .`when`()
            .command(command(orderId, emptyList()))
            .then()
            .resultMessagePayload(CompleteOrderCommandResult.cannotComplete())
            .noEvents()
    }
}
