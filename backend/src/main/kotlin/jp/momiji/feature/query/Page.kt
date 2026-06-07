package jp.momiji.feature.query

import kotlin.math.ceil

/** 一覧クエリの結果（1 ページ分）。 [items] と [paging] メタを持つ汎用コンテナ。 */
data class Page<T>(
    val items: List<T>,
    val paging: Paging,
)

/** ページングのメタ情報。 [totalPage] は総件数とページサイズから導出する。 */
data class Paging(
    val totalCount: Long,
    val pageSize: Int,
    val pageNumber: Int,
) {
    val totalPage: Int = if (pageSize <= 0) 0 else ceil(totalCount.toDouble() / pageSize).toInt()
}
