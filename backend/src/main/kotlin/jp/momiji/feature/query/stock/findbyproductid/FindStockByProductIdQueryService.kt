package jp.momiji.feature.query.stock.findbyproductid

import iss.jooq.generated.tables.references.STOCKS
import org.jooq.DSLContext
import org.springframework.stereotype.Component
import java.time.LocalDateTime

data class StockView(
    val productId: String,
    val onHand: Int,
    val reserved: Int,
    val updatedAt: LocalDateTime?,
) {
    /** 販売可能数 = 物理在庫 - 確保済み。 */
    val available: Int get() = onHand - reserved
}

@Component
class FindStockByProductIdQueryService(
    private val dsl: DSLContext,
) {
    /** 在庫レコードが無い商品は暗黙ゼロ（onHand=0 / reserved=0）として返す。 */
    fun findByProductId(productId: String): StockView =
        dsl
            .select(STOCKS.ON_HAND, STOCKS.RESERVED, STOCKS.UPDATED_AT)
            .from(STOCKS)
            .where(STOCKS.PRODUCT_ID.eq(productId))
            .fetchOne { record ->
                StockView(
                    productId = productId,
                    onHand = record[STOCKS.ON_HAND]!!,
                    reserved = record[STOCKS.RESERVED]!!,
                    updatedAt = record[STOCKS.UPDATED_AT],
                )
            }
            ?: StockView(productId = productId, onHand = 0, reserved = 0, updatedAt = null)
}
