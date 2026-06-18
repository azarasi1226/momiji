package jp.momiji.feature.command.order.preparepayment

import jp.momiji.MomijiIntegrationTestBase
import jp.momiji.event.order.OrderFailedEvent
import jp.momiji.event.order.OrderPaidEvent
import jp.momiji.event.order.OrderPaymentPreparedEvent
import jp.momiji.event.order.OrderStartedEvent
import org.junit.jupiter.api.Test

/**
 * 決済準備の記録（STARTED → PAYMENT_PENDING）を検証する。 注入するのは [jp.momiji.feature.command.order.OrderState]
 * （order_id 境界）だけなので、 given は Order* イベントだけで状態を組める。
 *
 * EventStore は全テストで共有・リセットされないため、 テストごとにユニークな id を使う。
 */
class PreparePaymentCommandHandlerTest : MomijiIntegrationTestBase() {
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

    private fun command(
        orderId: String,
        paymentMethodId: String = "pm_test",
        paymentIntentId: String = "pi_test",
    ) = PreparePaymentCommand(orderId = orderId, paymentMethodId = paymentMethodId, paymentIntentId = paymentIntentId)

    @Test
    fun `STARTED の注文を PAYMENT_PENDING にし OrderPaymentPrepared を発行する`() {
        val orderId = "01HXYZORDER0000000PREPARE01"
        val userId = "01HXYZUSER00000000PREPARE01"
        val p1 = "01HXYZPRODUCT0000PREPARE011"

        fixture
            .given()
            .events(orderStarted(orderId, userId, p1))
            .`when`()
            .command(command(orderId, paymentMethodId = "pm_PREP01", paymentIntentId = "pi_PREP01"))
            .then()
            .resultMessagePayload(PreparePaymentCommandResult.success())
            .events(
                OrderPaymentPreparedEvent(orderId = orderId, paymentMethodId = "pm_PREP01", paymentIntentId = "pi_PREP01"),
            )
    }

    @Test
    fun `注文が存在しないと orderNotFound で何も発行しない`() {
        val orderId = "01HXYZORDER0000000PREPARE02"

        fixture
            .given()
            .noPriorActivity()
            .`when`()
            .command(command(orderId))
            .then()
            .resultMessagePayload(PreparePaymentCommandResult.orderNotFound())
            .noEvents()
    }

    @Test
    fun `冪等性_既に PAYMENT_PENDING なら no-op 成功`() {
        val orderId = "01HXYZORDER0000000PREPARE03"
        val userId = "01HXYZUSER00000000PREPARE03"
        val p1 = "01HXYZPRODUCT0000PREPARE031"

        fixture
            .given()
            .events(
                orderStarted(orderId, userId, p1),
                OrderPaymentPreparedEvent(orderId = orderId, paymentMethodId = "pm_PREP03", paymentIntentId = "pi_PREP03"),
            ).`when`()
            .command(command(orderId, paymentMethodId = "pm_PREP03_RETRY", paymentIntentId = "pi_PREP03_RETRY"))
            .then()
            .resultMessagePayload(PreparePaymentCommandResult.success())
            .noEvents()
    }

    @Test
    fun `冪等性_既に PAID なら no-op 成功`() {
        val orderId = "01HXYZORDER0000000PREPARE04"
        val userId = "01HXYZUSER00000000PREPARE04"
        val p1 = "01HXYZPRODUCT0000PREPARE041"

        fixture
            .given()
            .events(
                orderStarted(orderId, userId, p1),
                OrderPaymentPreparedEvent(orderId = orderId, paymentMethodId = "pm_PREP04", paymentIntentId = "pi_PREP04"),
                OrderPaidEvent(orderId = orderId),
            ).`when`()
            .command(command(orderId))
            .then()
            .resultMessagePayload(PreparePaymentCommandResult.success())
            .noEvents()
    }

    @Test
    fun `冪等性_既に FAILED なら no-op 成功（再準備しない）`() {
        val orderId = "01HXYZORDER0000000PREPARE05"
        val userId = "01HXYZUSER00000000PREPARE05"
        val p1 = "01HXYZPRODUCT0000PREPARE051"

        fixture
            .given()
            .events(
                orderStarted(orderId, userId, p1),
                OrderFailedEvent(orderId = orderId, reason = "EXPIRED"),
            ).`when`()
            .command(command(orderId))
            .then()
            .resultMessagePayload(PreparePaymentCommandResult.success())
            .noEvents()
    }
}
