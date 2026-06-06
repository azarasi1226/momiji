package jp.momiji.feature.query.product.list

import iss.jooq.generated.tables.references.PRODUCTS
import jp.momiji.domain.product.ProductStatus
import jp.momiji.feature.query.Page
import jp.momiji.feature.query.Paging
import jp.momiji.feature.query.PagingCondition
import jp.momiji.feature.query.product.findbyid.ProductView
import org.jooq.DSLContext
import org.jooq.SortField
import org.jooq.impl.DSL
import org.springframework.stereotype.Component

enum class ProductSort {
    NAME_ASC,
    NAME_DESC,
    PRICE_ASC,
    PRICE_DESC,
    CREATED_AT_DESC,
    CREATED_AT_ASC,
}

data class ListProductsQuery(
    val likeName: String,
    val status: ProductStatus?,
    val brandId: String,
    val sort: ProductSort,
    val paging: PagingCondition,
)

@Component
class ListProductsQueryService(
    private val dsl: DSLContext,
) {
    fun list(query: ListProductsQuery): Page<ProductView> {
        // 検索: 空なら絞り込みなし
        val nameCondition =
            if (query.likeName.isBlank()) {
                DSL.noCondition()
            } else {
                PRODUCTS.NAME.like("%${query.likeName}%")
            }
        // 状態フィルタ: null なら絞り込みなし
        val statusCondition =
            if (query.status == null) {
                DSL.noCondition()
            } else {
                PRODUCTS.STATUS.eq(query.status.name)
            }
        // ブランドフィルタ: 空なら絞り込みなし
        val brandCondition =
            if (query.brandId.isBlank()) {
                DSL.noCondition()
            } else {
                PRODUCTS.BRAND_ID.eq(query.brandId)
            }
        val condition = nameCondition.and(statusCondition).and(brandCondition)

        // 総件数はウィンドウ関数 count() over() で 1 クエリにまとめて取る（別 count クエリを撃たない）。
        val totalCountField = DSL.count().over()

        val records =
            dsl
                .select(
                    PRODUCTS.ID,
                    PRODUCTS.BRAND_ID,
                    PRODUCTS.NAME,
                    PRODUCTS.DESCRIPTION,
                    PRODUCTS.IMAGE_URL,
                    PRODUCTS.PRICE,
                    PRODUCTS.STATUS,
                    PRODUCTS.CREATED_AT,
                    PRODUCTS.UPDATED_AT,
                    totalCountField,
                ).from(PRODUCTS)
                .where(condition)
                .orderBy(query.sort.toOrderField())
                .limit(query.paging.pageSize)
                .offset(query.paging.offset)
                .fetch()

        val totalCount = records.firstOrNull()?.get(totalCountField)?.toLong() ?: 0L
        val items =
            records.map { record ->
                ProductView(
                    id = record[PRODUCTS.ID]!!,
                    brandId = record[PRODUCTS.BRAND_ID]!!,
                    name = record[PRODUCTS.NAME]!!,
                    description = record[PRODUCTS.DESCRIPTION]!!,
                    imageUrl = record[PRODUCTS.IMAGE_URL],
                    price = record[PRODUCTS.PRICE]!!,
                    status = record[PRODUCTS.STATUS]!!,
                    createdAt = record[PRODUCTS.CREATED_AT]!!,
                    updatedAt = record[PRODUCTS.UPDATED_AT]!!,
                )
            }

        return Page(
            items = items,
            paging =
                Paging(
                    totalCount = totalCount,
                    pageSize = query.paging.pageSize,
                    pageNumber = query.paging.pageNumber,
                ),
        )
    }

    private fun ProductSort.toOrderField(): SortField<*> =
        when (this) {
            ProductSort.NAME_ASC -> PRODUCTS.NAME.asc()
            ProductSort.NAME_DESC -> PRODUCTS.NAME.desc()
            ProductSort.PRICE_ASC -> PRODUCTS.PRICE.asc()
            ProductSort.PRICE_DESC -> PRODUCTS.PRICE.desc()
            ProductSort.CREATED_AT_DESC -> PRODUCTS.CREATED_AT.desc()
            ProductSort.CREATED_AT_ASC -> PRODUCTS.CREATED_AT.asc()
        }
}
