package jp.momiji.adapter.payment

import com.stripe.model.Event
import com.stripe.model.SetupIntent
import com.stripe.net.Webhook
import io.github.oshai.kotlinlogging.KotlinLogging
import jp.momiji.port.payment.PaymentWebhookParser
import jp.momiji.port.payment.StripeWebhookEvent
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

/**
 * Stripe webhook の inbound 実装（[PaymentWebhookParser]）。
 *
 * 署名検証は `Webhook.constructEvent`（static）で行うため **StripeClient は不要**で、 webhook 署名 secret だけを
 * 必要とする。 これにより outbound の [StripePaymentGateway]（secret key）と credential が綺麗に分かれる。
 *
 * [parseWebhookEvent] は **薄いディスパッチャ**に徹し、 イベント種別ごとの解析は専用メソッドに切り出す。
 * 対応イベントを増やすときは `when` に 1 分岐 ＋ `parseXxx` メソッドを 1 個足す。
 */
@Component
@Profile("payment-stripe")
class StripeWebhookParser(
    @Value("\${momiji.stripe.webhook-secret}") private val webhookSecret: String,
) : PaymentWebhookParser {
    override fun parseWebhookEvent(
        payload: String,
        signature: String,
    ): StripeWebhookEvent {
        // 署名検証。 失敗時は SignatureVerificationException を投げ、 controller が 400 にする。
        val event = Webhook.constructEvent(payload, signature, webhookSecret)
        return when (event.type) {
            EVENT_SETUP_INTENT_SUCCEEDED -> parseSetupIntentSucceeded(event)
            // 今後: EVENT_PAYMENT_INTENT_SUCCEEDED -> parsePaymentIntentSucceeded(event) など
            else -> StripeWebhookEvent.Ignored
        }
    }

    /**
     * `setup_intent.succeeded` を解析する。 SetupIntent metadata の `user_id`（[STRIPE_METADATA_USER_ID]）から
     * 誰のカードかを復元し、 attach された pm_ と組にする。 必要情報が欠ける場合は [StripeWebhookEvent.Ignored]。
     */
    private fun parseSetupIntentSucceeded(event: Event): StripeWebhookEvent {
        // 期待した型のイベントで解析に失敗した場合、 黙って Ignored にするとカードが登録されないのに痕跡が
        // 残らない（サイレントなデータ欠落）。 早期 return には必ず warn ログを残す。
        val setupIntent =
            event.dataObjectDeserializer.getObject().orElse(null) as? SetupIntent
        if (setupIntent == null) {
            // 典型原因: Stripe アカウントの API バージョンと stripe-java の想定バージョンの不一致で
            // deserialization が失敗する（dataObjectDeserializer が empty を返す）。
            logger.warn { "setup_intent.succeeded の SetupIntent を復元できません（API バージョン不一致の可能性）: eventId=${event.id}" }
            return StripeWebhookEvent.Ignored
        }
        val userId = setupIntent.metadata?.get(STRIPE_METADATA_USER_ID)
        if (userId == null) {
            logger.warn { "SetupIntent に user_id metadata がありません: eventId=${event.id} setupIntentId=${setupIntent.id}" }
            return StripeWebhookEvent.Ignored
        }
        val paymentMethodId = setupIntent.paymentMethod
        if (paymentMethodId == null) {
            logger.warn { "SetupIntent に payment_method がありません: eventId=${event.id} setupIntentId=${setupIntent.id}" }
            return StripeWebhookEvent.Ignored
        }

        return StripeWebhookEvent.SetupIntentSucceeded(userId = userId, paymentMethodId = paymentMethodId)
    }

    companion object {
        private const val EVENT_SETUP_INTENT_SUCCEEDED = "setup_intent.succeeded"
    }
}
