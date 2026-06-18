package jp.momiji.feature.command.order.fail

import jp.momiji.domain.order.OrderFailureReason
import jp.momiji.feature.command.order.OrderProductIdsReader
import jp.momiji.feature.command.payment.StripeWebhookEventHandler
import jp.momiji.port.payment.StripeWebhookEvent
import org.axonframework.messaging.commandhandling.gateway.CommandGateway
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

/**
 * 注文の決済失敗 webhook（`payment_intent.payment_failed`）を処理する [StripeWebhookEventHandler]。
 *
 * **決済失敗＝注文失敗**の方針: [FailOrderCommand]（reason=PAYMENT_FAILED）で在庫予約を即解放して FAILED にする
 * （期限切れ sweeper と同じ補償に合流）。 ユーザーはカート（未クリア）から別カードで再チェックアウトできる。
 *
 * 外部（Stripe）が [FailOrderCommand] を起動する入口（inbound adapter）なので、 fail ユースケースと同居する。
 * FailOrder の整合境界（product_id）を組むため、 order_items（read model）から productId を読む（sweeper と同じ）。
 * コマンド側で「releasable か」を再ガードするので、 既に PAID/FAILED なら no-op（冪等）。
 */
@Component
@Profile("payment-stripe")
class OrderPaymentFailedWebhookHandler(
    private val commandGateway: CommandGateway,
    private val orderProductIdsReader: OrderProductIdsReader,
) : StripeWebhookEventHandler {
    override suspend fun handleIfSupported(event: StripeWebhookEvent) {
        if (event !is StripeWebhookEvent.PaymentIntentFailed) return

        // Projection が間に合わないのでは？ と思うかもしれないが、 決済準備（PayableOrderReader）で product_id を
        // アトミックに Query しており、 そこを通らなければここに到達しないので order_items は投影済みと保障される。
        commandGateway.failOrder(
            FailOrderCommand(
                orderId = event.orderId,
                productIds = orderProductIdsReader.read(event.orderId),
                reason = OrderFailureReason.PAYMENT_FAILED,
            ),
        )
    }
}
