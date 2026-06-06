package jp.momiji.feature.product.list

import iss.jooq.generated.tables.references.PRODUCTS
import jp.momiji.feature.product.findbyid.ProductView
import org.jooq.DSLContext
import org.springframework.stereotype.Component

@Component
class ListProductsQueryService(
    private val dsl: DSLContext,
) {
    fun findAll(): List<ProductView> =
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
            .orderBy(PRODUCTS.NAME.asc())
            .fetch { record ->
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
