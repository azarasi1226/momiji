package jp.momiji.feature.command.order.ship

import jp.momiji.MomijiIntegrationTestBase
import jp.momiji.event.order.OrderPaidEvent
import jp.momiji.event.order.OrderPaymentPreparedEvent
import jp.momiji.event.order.OrderShippedEvent
import jp.momiji.event.order.OrderStartedEvent
import org.junit.jupiter.api.Test

/**
 * 発送手続きの記録（PAID → SHIPPED）と冪等・ガードを検証する。 注入するのは
 * [jp.momiji.feature.command.order.OrderState]（order_id 境界）だけなので given は Order* イベントで組める。
 *
 * EventStore は全テストで共有・リセットされないため、 テストごとにユニークな id を使う。
 */
class ShipOrderCommandHandlerTest : MomijiIntegrationTestBase() {
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
        OrderPaymentPreparedEvent(orderId = orderId, paymentMethodId = "pm_SHIP", paymentIntentId = "pi_SHIP")

    private fun command(orderId: String) = ShipOrderCommand(orderId = orderId)

    @Test
    fun `PAID の注文を SHIPPED にし OrderShipped を発行する`() {
        val orderId = "01HXYZORDER000000000SHIP01"
        val userId = "01HXYZUSER0000000000SHIP01"
        val p1 = "01HXYZPRODUCT00000000SHIP011"

        fixture
            .given()
            .events(
                orderStarted(orderId, userId, p1),
                paymentPrepared(orderId),
                OrderPaidEvent(orderId = orderId),
            ).`when`()
            .command(command(orderId))
            .then()
            .resultMessagePayload(ShipOrderCommandResult.success())
            .events(OrderShippedEvent(orderId = orderId))
    }

    @Test
    fun `冪等性_既に SHIPPED なら no-op 成功`() {
        val orderId = "01HXYZORDER000000000SHIP02"
        val userId = "01HXYZUSER0000000000SHIP02"
        val p1 = "01HXYZPRODUCT00000000SHIP021"

        fixture
            .given()
            .events(
                orderStarted(orderId, userId, p1),
                paymentPrepared(orderId),
                OrderPaidEvent(orderId = orderId),
                OrderShippedEvent(orderId = orderId),
            ).`when`()
            .command(command(orderId))
            .then()
            .resultMessagePayload(ShipOrderCommandResult.success())
            .noEvents()
    }

    @Test
    fun `未決済（PAYMENT_PENDING）の注文は発送できず cannotShip`() {
        val orderId = "01HXYZORDER000000000SHIP03"
        val userId = "01HXYZUSER0000000000SHIP03"
        val p1 = "01HXYZPRODUCT00000000SHIP031"

        fixture
            .given()
            .events(
                orderStarted(orderId, userId, p1),
                paymentPrepared(orderId),
            ).`when`()
            .command(command(orderId))
            .then()
            .resultMessagePayload(ShipOrderCommandResult.cannotShip())
            .noEvents()
    }

    @Test
    fun `存在しない注文への発送は cannotShip`() {
        val orderId = "01HXYZORDER000000000SHIP04"

        fixture
            .given()
            .noPriorActivity()
            .`when`()
            .command(command(orderId))
            .then()
            .resultMessagePayload(ShipOrderCommandResult.cannotShip())
            .noEvents()
    }
}
