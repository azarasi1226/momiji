package jp.momiji.domain.order

/**
 * 注文のライフサイクル状態（注文機能たたき台の 5 状態）。
 *
 * read model（orders.status）には [name] を文字列で保存する（Enum 名の変更がイベント互換に影響しないよう
 * 文字列保存。 在庫の reason 等と同じ方針）。 現状の StartOrder は [STARTED] のみを書き込む。
 */
enum class OrderStatus {
    /** 注文開始・在庫予約済み（決済待ち）。 */
    STARTED,

    /** 決済成功。 */
    PAID,

    /** 発送済み。 */
    SHIPPED,

    /** 完了（在庫確定済み）。 */
    COMPLETED,

    /** 失敗（決済失敗・予約解放済み）。 */
    FAILED,
}
