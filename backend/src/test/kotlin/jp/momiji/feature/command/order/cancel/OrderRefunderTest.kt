package jp.momiji.feature.command.order.cancel

import io.mockk.mockk
import io.mockk.verify
import jp.momiji.event.order.OrderCancelledEvent
import jp.momiji.port.payment.PaymentGateway
import org.junit.jupiter.api.Test

class OrderRefunderTest {
    private val paymentGateway = mockk<PaymentGateway>(relaxed = true)
    private val refunder = OrderRefunder(paymentGateway)

    @Test
    fun `返金対象の pi が載っていれば返金する`() {
        refunder.on(
            OrderCancelledEvent(orderId = "order-1", reason = "CHANGED_MIND", refundPaymentIntentId = "pi_1"),
        )

        verify(exactly = 1) { paymentGateway.refundPayment("pi_1") }
    }

    @Test
    fun `pi が無ければ（未課金キャンセル）返金しない`() {
        refunder.on(
            OrderCancelledEvent(orderId = "order-2", reason = "CHANGED_MIND", refundPaymentIntentId = null),
        )

        verify(exactly = 0) { paymentGateway.refundPayment(any()) }
    }
}
