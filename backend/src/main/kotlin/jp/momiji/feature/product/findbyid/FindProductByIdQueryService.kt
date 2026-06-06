package jp.momiji.feature.product.findbyid

import iss.jooq.generated.tables.references.PRODUCTS
import org.jooq.DSLContext
import org.springframework.stereotype.Component
import java.time.LocalDateTime

data class ProductView(
    val id: String,
    val brandId: String,
    val name: String,
    val description: String,
    val imageUrl: String?,
    val price: Int,
    val status: String,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
)

@Component
class FindProductByIdQueryService(
    private val dsl: DSLContext,
) {
    fun findById(id: String): ProductView? =
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
            ).from(PRODUCTS)
            .where(PRODUCTS.ID.eq(id))
            .fetchOne { record ->
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
}
