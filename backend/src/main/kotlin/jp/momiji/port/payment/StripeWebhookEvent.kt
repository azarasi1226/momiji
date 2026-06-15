package jp.momiji.port.payment

/**
 * webhook を momiji が扱う形に正規化した **inbound イベント**。 Stripe の生イベント型に依存させない。
 *
 * [PaymentWebhookParser.parseWebhookEvent] が署名検証のうえでこの型に変換し、
 * [jp.momiji.feature.command.payment.StripeWebhookController] が各ハンドラに振り分ける。
 *
 * 対応する webhook イベントを増やすときは、 ここに sealed の subtype を足す（＋アダプタの parseWebhookEvent で
 * その event type を拾い、 担当機能スライスにハンドラを追加する）。
 */
sealed interface StripeWebhookEvent {
    /** setup_intent.succeeded。 [userId] は SetupIntent metadata 由来、 [paymentMethodId] は確定した pm_。 */
    data class SetupIntentSucceeded(
        val userId: String,
        val paymentMethodId: String,
    ) : StripeWebhookEvent

    /** payment_intent.succeeded（注文の決済成功）。 [orderId] は PaymentIntent metadata 由来、 [paymentIntentId] は pi_。 */
    data class PaymentIntentSucceeded(
        val orderId: String,
        val paymentIntentId: String,
    ) : StripeWebhookEvent

    /** payment_intent.payment_failed（注文の決済失敗）。 [orderId] は PaymentIntent metadata 由来。 */
    data class PaymentIntentFailed(
        val orderId: String,
    ) : StripeWebhookEvent

    /** 関心のないイベント（200 で無視する）。 */
    data object Ignored : StripeWebhookEvent
}
