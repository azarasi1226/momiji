package jp.momiji.feature.query.brand.list

import iss.jooq.generated.tables.references.BRANDS
import jp.momiji.feature.query.brand.findbyid.BrandView
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
                BRANDS.STATUS,
                BRANDS.CREATED_AT,
                BRANDS.UPDATED_AT,
            ).from(BRANDS)
            .orderBy(BRANDS.NAME.asc())
            .fetch { record ->
                BrandView(
                    id = record[BRANDS.ID]!!,
                    name = record[BRANDS.NAME]!!,
                    description = record[BRANDS.DESCRIPTION]!!,
                    status = record[BRANDS.STATUS]!!,
                    createdAt = record[BRANDS.CREATED_AT]!!,
                    updatedAt = record[BRANDS.UPDATED_AT]!!,
                )
            }
}
