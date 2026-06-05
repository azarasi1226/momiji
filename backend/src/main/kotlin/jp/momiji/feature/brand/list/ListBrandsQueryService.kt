package jp.momiji.feature.brand.list

import iss.jooq.generated.tables.references.BRANDS
import jp.momiji.feature.brand.findbyid.BrandView
import org.jooq.DSLContext
import org.springframework.stereotype.Component

@Component
class ListBrandsQueryService(
    private val dsl: DSLContext,
) {
    fun findAll(): List<BrandView> =
        dsl
            .select(
                BRANDS.ID,
                BRANDS.NAME,
                BRANDS.DESCRIPTION,
                BRANDS.CREATED_AT,
                BRANDS.UPDATED_AT,
            ).from(BRANDS)
            .orderBy(BRANDS.NAME.asc())
            .fetch { record ->
                BrandView(
                    id = record[BRANDS.ID]!!,
                    name = record[BRANDS.NAME]!!,
                    description = record[BRANDS.DESCRIPTION]!!,
                    createdAt = record[BRANDS.CREATED_AT]!!,
                    updatedAt = record[BRANDS.UPDATED_AT]!!,
                )
            }
}
