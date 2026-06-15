package jp.momiji.domain.order

/**
 * 注文が失敗（FAILED）に至った理由。 期限切れと（将来の）支払い失敗が同じ補償（確保解放＋失敗）へ合流するため、
 * どちらの理由で失敗したかをイベントに残す。
 *
 * イベントには [name] を文字列で載せる（Enum 名変更がイベント互換に影響しないよう文字列保存。 他イベントと同方針）。
 */
enum class OrderFailureReason {
    /** 予約タイムアウト（決済が時間内に完了しなかった）。 */
    EXPIRED,

    /** 決済失敗（将来の支払いフェーズで使用）。 */
    PAYMENT_FAILED,
}
