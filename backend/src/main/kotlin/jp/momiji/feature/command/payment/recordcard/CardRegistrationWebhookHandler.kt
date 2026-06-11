package jp.momiji.feature.command.payment.recordcard

import io.github.oshai.kotlinlogging.KotlinLogging
import jp.momiji.feature.command.payment.StripeWebhookEventHandler
import jp.momiji.port.payment.PaymentGateway
import jp.momiji.port.payment.StripeWebhookEvent
import org.axonframework.messaging.commandhandling.gateway.CommandGateway
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

/**
 * `setup_intent.succeeded` を受けてカードを記録する webhook ハンドラ。 カード登録の後半（非同期確定）を担う。
 *
 * pm_ の表示情報を Stripe から取得し [RecordCardRegistrationCommand] を送る。 webhook は再送されうるが
 * 記録コマンドは冪等。 業務エラー（例: setup 後にユーザー削除）は **永続的なので warn ログのみで握る**
 * （throw すると 500 → Stripe が無限リトライするため）。 想定外例外はここで握らず外へ伝播させ、
 * 500（= 一過性として Stripe が再送）に倒す。
 */
@Component
@Profile("payment-stripe")
class CardRegistrationWebhookHandler(
    private val paymentGateway: PaymentGateway,
    private val commandGateway: CommandGateway,
) : StripeWebhookEventHandler {
    override suspend fun handleIfSupported(event: StripeWebhookEvent) {
        if (event !is StripeWebhookEvent.SetupIntentSucceeded) return

        val card = paymentGateway.retrievePaymentMethod(event.paymentMethodId)
        val result =
            commandGateway.recordCardRegistration(
                RecordCardRegistrationCommand(
                    userId = event.userId,
                    paymentMethodId = event.paymentMethodId,
                    brand = card.brand,
                    last4 = card.last4,
                    expMonth = card.expMonth,
                    expYear = card.expYear,
                ),
            )
        if (!result.success) {
            // ユーザーが処理の合間に消された場合に到達する想定。
            // Eventを出す意味がないのでログに出すだけで終了
            logger.warn {
                "カード記録をスキップ（業務エラー・リトライ不要）: " +
                    "userId=${event.userId} paymentMethodId=${event.paymentMethodId} error=${result.error?.message}"
            }
        }
    }
}
