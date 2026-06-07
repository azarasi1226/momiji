package jp.momiji.domain.brand

/**
 * ブランドのライフサイクル状態。
 *
 * **「未作成」はここに入れない**（集約がまだ存在しないだけで、 状態ではない）。 書き込み側の
 * State では `BrandStatus?` の null で「未作成」を表す。
 */
enum class BrandStatus {
    /** 通常状態。 商品を新規に紐づけられる。 */
    ACTIVE,

    /** アーカイブ済み。 新規商品の紐づけ不可。 ハード削除はせず履歴・read model は残す。 */
    ARCHIVED,
}
