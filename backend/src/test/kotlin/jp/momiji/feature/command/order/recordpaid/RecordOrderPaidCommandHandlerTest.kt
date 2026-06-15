package jp.momiji.feature.command.order.recordpaid

import jp.momiji.MomijiIntegrationTestBase
import jp.momiji.event.order.OrderFailedEvent
import jp.momiji.event.order.OrderPaidEvent
import jp.momiji.event.order.OrderPaymentPreparedEvent
import jp.momiji.event.order.OrderStartedEvent
import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * 決済成功の記録（PAYMENT_PENDING → PAID）と、 履行不能時の返金要否を検証する。
 *
 * [RecordOrderPaidResult] は equals を持たないので、 ペイロードは [resultMessagePayloadSatisfies] で
 * refundRequired を検査する。 EventStore は全テストで共有・リセットされないため、 テストごとにユニークな id を使う。
 */
class RecordOrderPaidCommandHandlerTest : MomijiIntegrationTestBase() {
    private fun orderStarted(
        orderId: String,
        userId: String,
        productId: String,
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
                streetAddress = "1-1-1",
                building = "",
                deliveryNote = "",
            ),
        items = listOf(OrderStartedEvent.SnapshotItem(productId, "テスト商品", 1000, 1)),
    )

    private fun paymentPrepared(orderId: String) =
        OrderPaymentPreparedEvent(orderId = orderId, paymentMethodId = "pm_REC", paymentIntentId = "pi_REC")

    private fun command(orderId: String) = RecordOrderPaidCommand(orderId = orderId)

    @Test
    fun `PAYMENT_PENDING の注文を PAID にし OrderPaid を発行する（返金不要）`() {
        val orderId = "01HXYZORDER00000000RECORD01"
        val userId = "01HXYZUSER000000000RECORD01"
        val p1 = "01HXYZPRODUCT00000RECORD011"

        fixture
            .given()
            .events(
                orderStarted(orderId, userId, p1),
                paymentPrepared(orderId),
            ).`when`()
            .command(command(orderId))
            .then()
            .resultMessagePayloadSatisfies(RecordOrderPaidResult::class.java) { assertFalse(it.refundRequired) }
            .events(OrderPaidEvent(orderId = orderId))
    }

    @Test
    fun `冪等性_既に PAID なら no-op で返金不要`() {
        val orderId = "01HXYZORDER00000000RECORD02"
        val userId = "01HXYZUSER000000000RECORD02"
        val p1 = "01HXYZPRODUCT00000RECORD021"

        fixture
            .given()
            .events(
                orderStarted(orderId, userId, p1),
                paymentPrepared(orderId),
                OrderPaidEvent(orderId = orderId),
            ).`when`()
            .command(command(orderId))
            .then()
            .resultMessagePayloadSatisfies(RecordOrderPaidResult::class.java) { assertFalse(it.refundRequired) }
            .noEvents()
    }

    @Test
    fun `FAILED（期限切れ解放済み）の注文への決済成功は返金が必要`() {
        val orderId = "01HXYZORDER00000000RECORD03"
        val userId = "01HXYZUSER000000000RECORD03"
        val p1 = "01HXYZPRODUCT00000RECORD031"

        fixture
            .given()
            .events(
                orderStarted(orderId, userId, p1),
                paymentPrepared(orderId),
                // 期限切れで失効・在庫解放済み。
                OrderFailedEvent(orderId = orderId, reason = "EXPIRED"),
            ).`when`()
            .command(command(orderId))
            .then()
            .resultMessagePayloadSatisfies(RecordOrderPaidResult::class.java) { assertTrue(it.refundRequired) }
            .noEvents()
    }

    @Test
    fun `存在しない注文への決済成功は返金が必要`() {
        val orderId = "01HXYZORDER00000000RECORD04"

        fixture
            .given()
            .noPriorActivity()
            .`when`()
            .command(command(orderId))
            .then()
            .resultMessagePayloadSatisfies(RecordOrderPaidResult::class.java) { assertTrue(it.refundRequired) }
            .noEvents()
    }
}
