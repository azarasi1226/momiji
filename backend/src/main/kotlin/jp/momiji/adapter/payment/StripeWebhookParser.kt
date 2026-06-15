package jp.momiji.adapter.payment

import com.stripe.exception.EventDataObjectDeserializationException
import com.stripe.model.Event
import com.stripe.model.PaymentIntent
import com.stripe.model.SetupIntent
import com.stripe.model.StripeObject
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
            EVENT_PAYMENT_INTENT_SUCCEEDED -> parsePaymentIntentSucceeded(event)
            EVENT_PAYMENT_INTENT_FAILED -> parsePaymentIntentFailed(event)
            else -> StripeWebhookEvent.Ignored
        }
    }

    /** `payment_intent.succeeded` を解析する。 metadata の `order_id` と pi_ を組にする。 紐付け不能なら [StripeWebhookEvent.Ignored]。 */
    private fun parsePaymentIntentSucceeded(event: Event): StripeWebhookEvent {
        val paymentIntent = paymentIntentOf(event)
        val orderId = paymentIntent?.metadata?.get(STRIPE_METADATA_ORDER_ID)
        if (orderId == null) {
            // money-critical: 決済成功を注文に紐付けられない＝**課金済みなのに記録できない**。 このまま放置すると
            // 注文は PAYMENT_PENDING のまま sweeper が失効させ在庫を解放する（払ったのに失敗）。 我々が作る
            // PaymentIntent には必ず order_id を載せるので、 ここに来るのは異常。 error で必ず気付き、 pi_ で手動照合/返金する。
            logger.error {
                "payment_intent.succeeded を注文に紐付けられません（課金済みの可能性・要照合/返金）: " +
                    "eventId=${event.id} paymentIntentId=${paymentIntent?.id}"
            }
            return StripeWebhookEvent.Ignored
        }
        return StripeWebhookEvent.PaymentIntentSucceeded(orderId = orderId, paymentIntentId = paymentIntent.id)
    }

    /** `payment_intent.payment_failed` を解析する。 metadata の `order_id` を取り出す。 欠ける場合は [StripeWebhookEvent.Ignored]。 */
    private fun parsePaymentIntentFailed(event: Event): StripeWebhookEvent {
        val paymentIntent = paymentIntentOf(event) ?: return StripeWebhookEvent.Ignored
        val orderId = paymentIntent.metadata?.get(STRIPE_METADATA_ORDER_ID)
        if (orderId == null) {
            // 失敗イベントは**未課金**なので Ignore で無害（我々の注文なら TTL で sweeper が在庫解放する）。 warn 据え置き。
            logger.warn {
                "payment_intent.payment_failed に order_id metadata がありません: eventId=${event.id} paymentIntentId=${paymentIntent.id}"
            }
            return StripeWebhookEvent.Ignored
        }
        return StripeWebhookEvent.PaymentIntentFailed(orderId = orderId)
    }

    private fun paymentIntentOf(event: Event): PaymentIntent? = dataObjectOf(event) as? PaymentIntent

    /**
     * webhook の `data.object` を復元する。
     *
     * `getObject()` は payload の API バージョンが stripe-java の想定（[com.stripe.Stripe.API_VERSION]）と
     * 一致しないと空を返す（厳格）。 だが我々が読むのは **metadata と id だけ**で、 これらは全 API バージョンで
     * 安定なので、 [com.stripe.model.EventDataObjectDeserializer.deserializeUnsafe]（版非依存・best-effort）で取る。
     * これにより Stripe アカウント / webhook endpoint の API バージョンに依存しなくなる。
     */
    private fun dataObjectOf(event: Event): StripeObject? =
        try {
            event.dataObjectDeserializer.deserializeUnsafe()
        } catch (e: EventDataObjectDeserializationException) {
            logger.warn(e) { "${event.type} の data.object を復元できません: eventId=${event.id}" }
            null
        }

    /**
     * `setup_intent.succeeded` を解析する。 SetupIntent metadata の `user_id`（[STRIPE_METADATA_USER_ID]）から
     * 誰のカードかを復元し、 attach された pm_ と組にする。 必要情報が欠ける場合は [StripeWebhookEvent.Ignored]。
     */
    private fun parseSetupIntentSucceeded(event: Event): StripeWebhookEvent {
        // 期待した型のイベントで解析に失敗した場合、 黙って Ignored にするとカードが登録されないのに痕跡が
        // 残らない（サイレントなデータ欠落）。 早期 return には必ず warn ログを残す（[dataObjectOf] が記録する）。
        val setupIntent = dataObjectOf(event) as? SetupIntent ?: return StripeWebhookEvent.Ignored
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
        private const val EVENT_PAYMENT_INTENT_SUCCEEDED = "payment_intent.succeeded"
        private const val EVENT_PAYMENT_INTENT_FAILED = "payment_intent.payment_failed"
    }
}
