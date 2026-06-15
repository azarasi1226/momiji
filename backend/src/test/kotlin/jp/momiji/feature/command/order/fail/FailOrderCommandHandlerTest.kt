package jp.momiji.feature.command.order.fail

import jp.momiji.MomijiIntegrationTestBase
import jp.momiji.domain.order.OrderFailureReason
import jp.momiji.event.order.OrderFailedEvent
import jp.momiji.event.order.OrderPaidEvent
import jp.momiji.event.order.OrderPaymentPreparedEvent
import jp.momiji.event.order.OrderStartedEvent
import jp.momiji.event.product.ProductCreatedEvent
import jp.momiji.event.stock.StockReceivedEvent
import jp.momiji.event.stock.StockReservationReleasedEvent
import jp.momiji.event.stock.StockReservedEvent
import org.junit.jupiter.api.Test

class FailOrderCommandHandlerTest : MomijiIntegrationTestBase() {
    // EventStore は全テストで共有・リセットされないため、テストごとにユニークな id を使う。

    private fun productCreated(productId: String) =
        ProductCreatedEvent(
            id = productId,
            brandId = "01HXYZBRAND0000000000FAIL1",
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

    private fun command(
        orderId: String,
        productIds: List<String>,
        reason: OrderFailureReason,
    ) = FailOrderCommand(orderId = orderId, productIds = productIds, reason = reason)

    @Test
    fun `期限切れ_STARTED の注文を失効させ、 予約解放と OrderFailed(EXPIRED) を発行する`() {
        val orderId = "01HXYZORDER00000000FAIL01"
        val userId = "01HXYZUSER000000000FAILU01"
        val p1 = "01HXYZPRODUCT0000000FAIL011"

        fixture
            .given()
            .events(
                productCreated(p1),
                stockReceived(p1, onHand = 10),
                StockReservedEvent(productId = p1, orderId = orderId, quantity = 2, reservedQuantity = 2),
                orderStarted(orderId, userId, p1, quantity = 2),
            ).`when`()
            .command(command(orderId, listOf(p1), OrderFailureReason.EXPIRED))
            .then()
            .resultMessagePayload(FailOrderCommandResult.success())
            .events(
                OrderFailedEvent(orderId = orderId, reason = "EXPIRED"),
                StockReservationReleasedEvent(productId = p1, orderId = orderId, quantity = 2, reservedQuantity = 0),
            )
    }

    @Test
    fun `支払い失敗_PAYMENT_PENDING の注文を解放し OrderFailed(PAYMENT_FAILED) を発行する`() {
        val orderId = "01HXYZORDER00000000FAIL02"
        val userId = "01HXYZUSER000000000FAILU02"
        val p1 = "01HXYZPRODUCT0000000FAIL021"

        fixture
            .given()
            .events(
                productCreated(p1),
                stockReceived(p1, onHand = 10),
                StockReservedEvent(productId = p1, orderId = orderId, quantity = 3, reservedQuantity = 3),
                orderStarted(orderId, userId, p1, quantity = 3),
                // 決済着手済み（PAYMENT_PENDING）。
                OrderPaymentPreparedEvent(orderId = orderId, paymentMethodId = "pm_FAIL02", paymentIntentId = "pi_FAIL02"),
            ).`when`()
            .command(command(orderId, listOf(p1), OrderFailureReason.PAYMENT_FAILED))
            .then()
            .resultMessagePayload(FailOrderCommandResult.success())
            .events(
                OrderFailedEvent(orderId = orderId, reason = "PAYMENT_FAILED"),
                StockReservationReleasedEvent(productId = p1, orderId = orderId, quantity = 3, reservedQuantity = 0),
            )
    }

    @Test
    fun `PAID の注文は解放しない（paid-before-expiry 保護）`() {
        val orderId = "01HXYZORDER00000000FAIL03"
        val userId = "01HXYZUSER000000000FAILU03"
        val p1 = "01HXYZPRODUCT0000000FAIL031"

        fixture
            .given()
            .events(
                productCreated(p1),
                stockReceived(p1, onHand = 10),
                StockReservedEvent(productId = p1, orderId = orderId, quantity = 2, reservedQuantity = 2),
                orderStarted(orderId, userId, p1, quantity = 2),
                OrderPaymentPreparedEvent(orderId = orderId, paymentMethodId = "pm_FAIL03", paymentIntentId = "pi_FAIL03"),
                // 既に決済成功済み。
                OrderPaidEvent(orderId = orderId),
            ).`when`()
            .command(command(orderId, listOf(p1), OrderFailureReason.EXPIRED))
            .then()
            .resultMessagePayload(FailOrderCommandResult.success())
            .noEvents()
    }

    @Test
    fun `冪等_既に FAILED の注文には何もしない`() {
        val orderId = "01HXYZORDER00000000FAIL04"
        val userId = "01HXYZUSER000000000FAILU04"
        val p1 = "01HXYZPRODUCT0000000FAIL041"

        fixture
            .given()
            .events(
                productCreated(p1),
                stockReceived(p1, onHand = 10),
                StockReservedEvent(productId = p1, orderId = orderId, quantity = 2, reservedQuantity = 2),
                orderStarted(orderId, userId, p1, quantity = 2),
                OrderFailedEvent(orderId = orderId, reason = "EXPIRED"),
            ).`when`()
            .command(command(orderId, listOf(p1), OrderFailureReason.EXPIRED))
            .then()
            .resultMessagePayload(FailOrderCommandResult.success())
            .noEvents()
    }
}
