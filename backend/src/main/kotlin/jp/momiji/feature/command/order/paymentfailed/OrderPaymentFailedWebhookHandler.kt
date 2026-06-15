package jp.momiji.feature.command.order.paymentfailed

import iss.jooq.generated.tables.references.ORDER_ITEMS
import jp.momiji.domain.order.OrderFailureReason
import jp.momiji.feature.command.order.fail.FailOrderCommand
import jp.momiji.feature.command.order.fail.failOrder
import jp.momiji.feature.command.payment.StripeWebhookEventHandler
import jp.momiji.port.payment.StripeWebhookEvent
import org.axonframework.messaging.commandhandling.gateway.CommandGateway
import org.jooq.DSLContext
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

/**
 * 注文の決済失敗 webhook（`payment_intent.payment_failed`）を処理する [StripeWebhookEventHandler]。
 *
 * **決済失敗＝注文失敗**の方針: [FailOrderCommand]（reason=PAYMENT_FAILED）で在庫予約を即解放して FAILED にする
 * （期限切れ sweeper と同じ補償に合流）。 ユーザーはカート（未クリア）から別カードで再チェックアウトできる。
 *
 * FailOrder の整合境界（product_id）を組むため、 order_items（read model）から productId を読む（sweeper と同じ）。
 * コマンド側で「releasable か」を再ガードするので、 既に PAID/FAILED なら no-op（冪等）。
 */
@Component
@Profile("payment-stripe")
class OrderPaymentFailedWebhookHandler(
    private val commandGateway: CommandGateway,
    private val dsl: DSLContext,
) : StripeWebhookEventHandler {
    override suspend fun handleIfSupported(event: StripeWebhookEvent) {
        if (event !is StripeWebhookEvent.PaymentIntentFailed) return

        commandGateway.failOrder(
            FailOrderCommand(
                orderId = event.orderId,
                productIds = findOrderProductIds(event.orderId),
                reason = OrderFailureReason.PAYMENT_FAILED,
            ),
        )
    }

    // ここで Projection が間に合わず、適切にアイテムが解放されないのではないか？と思うかもしれませんが、
    // 決済準備の段階で([PayableOrderReader]の中で) ProductIds を アトミックに Queryしている箇所があり、その処理が通らなければここに到達しないので、 Projection されていると保障されている。
    private fun findOrderProductIds(orderId: String): List<String> =
        dsl
            .select(ORDER_ITEMS.PRODUCT_ID)
            .from(ORDER_ITEMS)
            .where(ORDER_ITEMS.ORDER_ID.eq(orderId))
            .fetch(ORDER_ITEMS.PRODUCT_ID)
            .filterNotNull()
}
