package jp.momiji.query

/**
 * ページング条件（一覧クエリの入力）。 read/query 横断の関心事なので domain でなく query 層に置く。
 *
 * **常に有効な値であること**をコンストラクタで保証する（`internal constructor` + [of] ファクトリのみ）。
 * 一覧は不正入力を**拒否でなく正規化（clamp）**する方針: ページ番号が無ければ 1 ページ目、 サイズ未指定は
 * 既定、 過大は上限に丸める（クライアントに paging のバリデーションエラーを返さない）。
 */
data class PagingCondition internal constructor(
    val pageSize: Int,
    val pageNumber: Int,
) {
    /** LIMIT/OFFSET の offset。 pageNumber は 1 始まりなので (pageNumber - 1) * pageSize。 */
    val offset: Int get() = (pageNumber - 1) * pageSize

    companion object {
        const val DEFAULT_PAGE_SIZE = 20
        const val MAX_PAGE_SIZE = 100

        /**
         * クライアント入力を正規化して**常に有効な** [PagingCondition] を返す。
         * - pageSize: 0 以下 → [DEFAULT_PAGE_SIZE]、 [MAX_PAGE_SIZE] 超 → 上限
         * - pageNumber: 1 未満 → 1
         */
        fun of(
            pageSize: Int,
            pageNumber: Int,
        ): PagingCondition {
            val size =
                when {
                    pageSize <= 0 -> DEFAULT_PAGE_SIZE
                    pageSize > MAX_PAGE_SIZE -> MAX_PAGE_SIZE
                    else -> pageSize
                }
            val number = if (pageNumber < 1) 1 else pageNumber
            return PagingCondition(size, number)
        }
    }
}
