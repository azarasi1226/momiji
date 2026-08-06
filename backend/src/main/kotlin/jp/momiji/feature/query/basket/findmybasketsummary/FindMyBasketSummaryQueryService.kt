package jp.momiji.feature.query.basket.findmybasketsummary

import iss.jooq.generated.tables.references.BASKETS
import iss.jooq.generated.tables.references.PRODUCTS
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.stereotype.Component

data class BasketSummaryView(
    val totalQuantity: Int,
    val totalTypeCount: Int,
    val totalPrice: Long,
)

@Component
class FindMyBasketSummaryQueryService(
    private val dsl: DSLContext,
) {
    fun summarize(userId: String): BasketSummaryView {
        // 種類
        val typeCount = DSL.count()
        // 合計個数
        val quantitySum = DSL.sum(BASKETS.ITEM_QUANTITY)
        // 合計値段
        val priceSum = DSL.sum(PRODUCTS.PRICE.mul(BASKETS.ITEM_QUANTITY))

        val record =
            dsl
                .select(typeCount, quantitySum, priceSum)
                .from(BASKETS)
                .join(PRODUCTS)
                .on(BASKETS.PRODUCT_ID.eq(PRODUCTS.ID))
                .where(BASKETS.USER_ID.eq(userId))
                .fetchOne()

        return BasketSummaryView(
            totalQuantity = record?.get(quantitySum)?.toInt() ?: 0,
            totalTypeCount = record?.get(typeCount) ?: 0,
            totalPrice = record?.get(priceSum)?.toLong() ?: 0L,
        )
    }
}
