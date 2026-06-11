package jp.momiji.feature.command.payment

import jp.momiji.port.payment.StripeWebhookEvent

/**
 * Stripe webhook の 1 イベントを処理するハンドラ。
 *
 * Stripe は 1 エンドポイントで全イベント種別（`setup_intent.succeeded` / 将来の `payment_intent.succeeded` /
 * `charge.refunded` 等）をまとめて受ける。 [StripeWebhookController] は機能に依存しない振り分け役に徹し、
 * 各ユースケースは自分の関心のイベントだけを拾う実装をこの interface で提供する（実装は各 use-case パッケージに置く）。
 *
 * これで Controller を触らずに、 機能スライス側にハンドラを足すだけで対応イベントを増やせる。
 */
interface StripeWebhookEventHandler {
    /**
     * 自分が関心のあるイベントなら処理し、 そうでなければ何もしない。
     * （対応外イベントの無視は副作用なしであるべき）
     */
    suspend fun handleIfSupported(event: StripeWebhookEvent)
}
