package jp.momiji.domain.order

/**
 * ユーザーが注文をキャンセルした理由。 キャンセルは理由の選択を必須にするため、 イベントに残す。
 *
 * イベントには [name] を文字列で載せる（Enum 名変更がイベント互換に影響しないよう文字列保存。 [OrderFailureReason] と同方針）。
 */
enum class OrderCancellationReason {
    /** 気が変わった・不要になった。 */
    CHANGED_MIND,

    /** 間違えて注文した。 */
    ORDERED_BY_MISTAKE,

    /** 他でより良い条件（価格など）を見つけた。 */
    FOUND_BETTER_PRICE,

    /** 届くのが遅い。 */
    DELIVERY_TOO_SLOW,

    /** その他。 */
    OTHER,
}
