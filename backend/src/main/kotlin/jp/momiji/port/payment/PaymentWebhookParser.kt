package jp.momiji.port.payment

/**
 * Stripe webhook の **inbound** ポート。 生 payload と署名ヘッダを検証し、 [StripeWebhookEvent] に正規化する。
 *
 * 「こちらが Stripe を叩く」 outbound 操作の [PaymentGateway] とは向き・関心・必要 credential が異なるため
 * 別 interface に分離している（[PaymentGateway] は secret key、 こちらは webhook 署名 secret だけを必要とする）。
 * 消費者は [jp.momiji.feature.command.payment.StripeWebhookController] のみ。
 */
interface PaymentWebhookParser {
    /**
     * webhook の生 payload と署名ヘッダを検証し、 関心のあるイベントに正規化して返す。
     * 署名検証に失敗した場合は例外を投げる（呼び出し側で 400 にする）。
     */
    fun parseWebhookEvent(
        payload: String,
        signature: String,
    ): StripeWebhookEvent
}
