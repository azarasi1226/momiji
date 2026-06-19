package jp.momiji.feature.command.order.cancel

import jp.momiji.MomijiIntegrationTestBase
import jp.momiji.domain.order.OrderCancellationReason
import jp.momiji.event.order.OrderCancelledEvent
import jp.momiji.event.order.OrderPaidEvent
import jp.momiji.event.order.OrderPaymentPreparedEvent
import jp.momiji.event.order.OrderShippedEvent
import jp.momiji.event.order.OrderStartedEvent
import jp.momiji.event.product.ProductCreatedEvent
import jp.momiji.event.stock.StockReceivedEvent
import jp.momiji.event.stock.StockReservationReleasedEvent
import jp.momiji.event.stock.StockReservedEvent
import org.junit.jupiter.api.Test

class CancelOrderCommandHandlerTest : MomijiIntegrationTestBase() {
    // EventStore は全テストで共有・リセットされないため、テストごとにユニークな id を使う。

    private fun productCreated(productId: String) =
        ProductCreatedEvent(
            id = productId,
            brandId = "01HXYZBRAND000000000CANCL1",
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
        reason: OrderCancellationReason = OrderCancellationReason.CHANGED_MIND,
    ) = CancelOrderCommand(orderId = orderId, productIds = productIds, reason = reason)

    @Test
    fun `STARTED の注文をキャンセルし、 予約解放と OrderCancelled（返金なし）を発行する`() {
        val orderId = "01HXYZORDER0000000CANCL01"
        val userId = "01HXYZUSER00000000CANCLU01"
        val p1 = "01HXYZPRODUCT000000CANCL011"

        fixture
            .given()
            .events(
                productCreated(p1),
                stockReceived(p1, onHand = 10),
                StockReservedEvent(productId = p1, orderId = orderId, quantity = 2, reservedQuantity = 2),
                orderStarted(orderId, userId, p1, quantity = 2),
            ).`when`()
            .command(command(orderId, listOf(p1), OrderCancellationReason.ORDERED_BY_MISTAKE))
            .then()
            .resultMessagePayload(CancelOrderCommandResult.success())
            .events(
                OrderCancelledEvent(orderId = orderId, reason = "ORDERED_BY_MISTAKE", refundPaymentIntentId = null),
                StockReservationReleasedEvent(productId = p1, orderId = orderId, quantity = 2, reservedQuantity = 0),
            )
    }

    @Test
    fun `PAID の注文をキャンセルし、 予約解放と OrderCancelled（返金pi付き）を発行する`() {
        val orderId = "01HXYZORDER0000000CANCL02"
        val userId = "01HXYZUSER00000000CANCLU02"
        val p1 = "01HXYZPRODUCT000000CANCL021"

        fixture
            .given()
            .events(
                productCreated(p1),
                stockReceived(p1, onHand = 10),
                StockReservedEvent(productId = p1, orderId = orderId, quantity = 2, reservedQuantity = 2),
                orderStarted(orderId, userId, p1, quantity = 2),
                OrderPaymentPreparedEvent(orderId = orderId, paymentMethodId = "pm_CANCL02", paymentIntentId = "pi_CANCL02"),
                OrderPaidEvent(orderId = orderId),
            ).`when`()
            .command(command(orderId, listOf(p1), OrderCancellationReason.CHANGED_MIND))
            .then()
            .resultMessagePayload(CancelOrderCommandResult.success())
            .events(
                // 課金確定済みなので返金対象の pi_ が載る。
                OrderCancelledEvent(orderId = orderId, reason = "CHANGED_MIND", refundPaymentIntentId = "pi_CANCL02"),
                StockReservationReleasedEvent(productId = p1, orderId = orderId, quantity = 2, reservedQuantity = 0),
            )
    }

    @Test
    fun `PAYMENT_PENDING の注文をキャンセルし、 予約解放と OrderCancelled（返金なし）を発行する`() {
        val orderId = "01HXYZORDER0000000CANCL03"
        val userId = "01HXYZUSER00000000CANCLU03"
        val p1 = "01HXYZPRODUCT000000CANCL031"

        fixture
            .given()
            .events(
                productCreated(p1),
                stockReceived(p1, onHand = 10),
                StockReservedEvent(productId = p1, orderId = orderId, quantity = 1, reservedQuantity = 1),
                orderStarted(orderId, userId, p1, quantity = 1),
                OrderPaymentPreparedEvent(orderId = orderId, paymentMethodId = "pm_CANCL03", paymentIntentId = "pi_CANCL03"),
            ).`when`()
            .command(command(orderId, listOf(p1)))
            .then()
            .resultMessagePayload(CancelOrderCommandResult.success())
            .events(
                // 未課金（PAYMENT_PENDING）なので返金しない（pi_ は載せない）。
                OrderCancelledEvent(orderId = orderId, reason = "CHANGED_MIND", refundPaymentIntentId = null),
                StockReservationReleasedEvent(productId = p1, orderId = orderId, quantity = 1, reservedQuantity = 0),
            )
    }

    @Test
    fun `発送済み（SHIPPED）の注文はキャンセルできない`() {
        val orderId = "01HXYZORDER0000000CANCL04"
        val userId = "01HXYZUSER00000000CANCLU04"
        val p1 = "01HXYZPRODUCT000000CANCL041"

        fixture
            .given()
            .events(
                productCreated(p1),
                stockReceived(p1, onHand = 10),
                StockReservedEvent(productId = p1, orderId = orderId, quantity = 2, reservedQuantity = 2),
                orderStarted(orderId, userId, p1, quantity = 2),
                OrderPaymentPreparedEvent(orderId = orderId, paymentMethodId = "pm_CANCL04", paymentIntentId = "pi_CANCL04"),
                OrderPaidEvent(orderId = orderId),
                OrderShippedEvent(orderId = orderId),
            ).`when`()
            .command(command(orderId, listOf(p1)))
            .then()
            .resultMessagePayload(CancelOrderCommandResult.alreadyShipped())
            .noEvents()
    }

    @Test
    fun `冪等_既に CANCELLED の注文には何もしない`() {
        val orderId = "01HXYZORDER0000000CANCL05"
        val userId = "01HXYZUSER00000000CANCLU05"
        val p1 = "01HXYZPRODUCT000000CANCL051"

        fixture
            .given()
            .events(
                productCreated(p1),
                stockReceived(p1, onHand = 10),
                StockReservedEvent(productId = p1, orderId = orderId, quantity = 2, reservedQuantity = 2),
                orderStarted(orderId, userId, p1, quantity = 2),
                OrderCancelledEvent(orderId = orderId, reason = "CHANGED_MIND", refundPaymentIntentId = null),
            ).`when`()
            .command(command(orderId, listOf(p1)))
            .then()
            .resultMessagePayload(CancelOrderCommandResult.success())
            .noEvents()
    }
}
