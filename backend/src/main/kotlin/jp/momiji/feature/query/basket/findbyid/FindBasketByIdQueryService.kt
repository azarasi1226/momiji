package jp.momiji.feature.query.basket.findbyid

import iss.jooq.generated.tables.references.BASKETS
import iss.jooq.generated.tables.references.PRODUCTS
import jp.momiji.feature.query.Page
import jp.momiji.feature.query.Paging
import jp.momiji.feature.query.PagingCondition
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.stereotype.Component

data class BasketItemView(
    val productId: String,
    val productName: String,
    val productPrice: Int,
    val productImageUrl: String?,
    val itemQuantity: Int,
)

data class FindBasketByIdQuery(
    val userId: String,
    val paging: PagingCondition,
)

@Component
class FindBasketByIdQueryService(
    private val dsl: DSLContext,
) {
    fun findById(query: FindBasketByIdQuery): Page<BasketItemView> {
        // 総件数はウィンドウ関数 count() over() で 1 クエリにまとめて取る。
        val totalCountField = DSL.count().over()

        val records =
            dsl
                .select(
                    BASKETS.PRODUCT_ID,
                    PRODUCTS.NAME,
                    PRODUCTS.PRICE,
                    PRODUCTS.IMAGE_URL,
                    BASKETS.ITEM_QUANTITY,
                    totalCountField,
                ).from(BASKETS)
                .join(PRODUCTS)
                .on(BASKETS.PRODUCT_ID.eq(PRODUCTS.ID))
                .where(BASKETS.USER_ID.eq(query.userId))
                .orderBy(BASKETS.ADDED_AT.asc())
                .limit(query.paging.pageSize)
                .offset(query.paging.offset)
                .fetch()

        val totalCount = records.firstOrNull()?.get(totalCountField)?.toLong() ?: 0L
        val items =
            records.map { record ->
                BasketItemView(
                    productId = record[BASKETS.PRODUCT_ID]!!,
                    productName = record[PRODUCTS.NAME]!!,
                    productPrice = record[PRODUCTS.PRICE]!!,
                    productImageUrl = record[PRODUCTS.IMAGE_URL],
                    itemQuantity = record[BASKETS.ITEM_QUANTITY]!!,
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
}
