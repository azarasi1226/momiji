package jp.momiji.projection.product

import iss.jooq.generated.tables.references.PRODUCTS
import jp.momiji.domain.product.ProductStatus
import jp.momiji.event.product.ProductCreatedEvent
import jp.momiji.event.product.ProductDiscontinuedEvent
import jp.momiji.event.product.ProductUpdatedEvent
import jp.momiji.feature.InitialPosition
import org.axonframework.messaging.eventhandling.annotation.EventHandler
import org.axonframework.messaging.eventhandling.annotation.Timestamp
import org.jooq.DSLContext
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

@Component
class ProductTableProjector(
    private val dsl: DSLContext,
) {
    @EventHandler
    fun on(
        event: ProductCreatedEvent,
        @Timestamp timestamp: Instant,
    ) {
        val at = LocalDateTime.ofInstant(timestamp, ZoneId.systemDefault())
        dsl
            .insertInto(PRODUCTS)
            .set(PRODUCTS.ID, event.id)
            .set(PRODUCTS.BRAND_ID, event.brandId)
            .set(PRODUCTS.NAME, event.name)
            .set(PRODUCTS.DESCRIPTION, event.description)
            .set(PRODUCTS.IMAGE_URL, event.imageUrl)
            .set(PRODUCTS.PRICE, event.price)
            .set(PRODUCTS.STATUS, ProductStatus.ACTIVE.name)
            .set(PRODUCTS.CREATED_AT, at)
            .set(PRODUCTS.UPDATED_AT, at)
            .onDuplicateKeyIgnore()
            .execute()
    }

    @EventHandler
    fun on(
        event: ProductUpdatedEvent,
        @Timestamp timestamp: Instant,
    ) {
        dsl
            .update(PRODUCTS)
            .set(PRODUCTS.NAME, event.name)
            .set(PRODUCTS.DESCRIPTION, event.description)
            .set(PRODUCTS.IMAGE_URL, event.imageUrl)
            .set(PRODUCTS.PRICE, event.price)
            .set(PRODUCTS.UPDATED_AT, LocalDateTime.ofInstant(timestamp, ZoneId.systemDefault()))
            .where(PRODUCTS.ID.eq(event.id))
            .execute()
    }

    @EventHandler
    fun on(
        event: ProductDiscontinuedEvent,
        @Timestamp timestamp: Instant,
    ) {
        dsl
            .update(PRODUCTS)
            .set(PRODUCTS.STATUS, ProductStatus.DISCONTINUED.name)
            .set(PRODUCTS.UPDATED_AT, LocalDateTime.ofInstant(timestamp, ZoneId.systemDefault()))
            .where(PRODUCTS.ID.eq(event.id))
            .execute()
    }
}
