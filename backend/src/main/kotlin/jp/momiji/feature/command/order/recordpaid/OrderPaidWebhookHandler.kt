package jp.momiji.feature.command.order.recordpaid

import jp.momiji.feature.command.payment.StripeWebhookEventHandler
import jp.momiji.port.payment.PaymentGateway
import jp.momiji.port.payment.StripeWebhookEvent
import org.axonframework.messaging.commandhandling.gateway.CommandGateway
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

/**
 * 注文の決済成功 webhook（`payment_intent.succeeded`）を処理する [StripeWebhookEventHandler]。
 *
 * [RecordOrderPaidCommand] で PAID に確定する。 既に期限切れで解放済み（FAILED/不在）なら **返金**（paid-after-expiry のバックストップ）。
 * 駆動するコマンド（recordpaid）の隣に置く。
 */
@Component
@Profile("payment-stripe")
class OrderPaidWebhookHandler(
    private val commandGateway: CommandGateway,
    private val paymentGateway: PaymentGateway,
) : StripeWebhookEventHandler {
    override suspend fun handleIfSupported(event: StripeWebhookEvent) {
        if (event !is StripeWebhookEvent.PaymentIntentSucceeded) return

        // 支払い状態を記録 or 返金対応が必要かを返却する
        val result = commandGateway.recordOrderPaid(RecordOrderPaidCommand(orderId = event.orderId))
        if (result.refundRequired) {
            // ユーザーが、3d secureに時間がかかったなどの理由でオーダーが締め切り状態になっているのであれば、返金する。
            paymentGateway.refundPayment(event.paymentIntentId)
        }
    }
}
