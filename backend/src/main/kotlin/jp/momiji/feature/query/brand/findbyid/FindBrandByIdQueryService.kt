package jp.momiji.feature.query.brand.findbyid

import iss.jooq.generated.tables.references.BRANDS
import org.jooq.DSLContext
import org.springframework.stereotype.Component
import java.time.LocalDateTime

data class BrandView(
    val id: String,
    val name: String,
    val description: String,
    val status: String,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
)

@Component
class FindBrandByIdQueryService(
    private val dsl: DSLContext,
) {
    fun findById(id: String): BrandView? =
        dsl
            .select(
                BRANDS.ID,
                BRANDS.NAME,
                BRANDS.DESCRIPTION,
                BRANDS.STATUS,
                BRANDS.CREATED_AT,
                BRANDS.UPDATED_AT,
            ).from(BRANDS)
            .where(BRANDS.ID.eq(id))
            .fetchOne { record ->
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
