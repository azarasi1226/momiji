package jp.momiji.domain.product

/**
 * 商品のライフサイクル状態。
 *
 * **「未作成」はここに入れない**（集約がまだ存在しないだけで、 状態ではない）。 書き込み側の
 * State では `ProductStatus?` の null で「未作成」を表す。
 *
 * 「在庫切れ」は Stock の数量から**導出**される値であって商品の状態ではないため、 ここには持たない。
 */
enum class ProductStatus {
    /** 販売中。 更新可能。 */
    ACTIVE,

    /** 生産終了（廃番）。 ハード削除はせず履歴・read model は残す。 更新不可。 */
    DISCONTINUED,
}
