package jp.momiji.projection.brand

import iss.jooq.generated.tables.references.BRANDS
import jp.momiji.domain.brand.BrandStatus
import jp.momiji.event.brand.BrandArchivedEvent
import jp.momiji.event.brand.BrandCreatedEvent
import jp.momiji.event.brand.BrandUpdatedEvent
import org.axonframework.messaging.eventhandling.annotation.EventHandler
import org.axonframework.messaging.eventhandling.annotation.Timestamp
import org.jooq.DSLContext
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

@Component
class BrandTableProjector(
    private val dsl: DSLContext,
) {
    @EventHandler
    fun on(
        event: BrandCreatedEvent,
        @Timestamp timestamp: Instant,
    ) {
        val at = LocalDateTime.ofInstant(timestamp, ZoneOffset.UTC)
        dsl
            .insertInto(BRANDS)
            .set(BRANDS.ID, event.id)
            .set(BRANDS.NAME, event.name)
            .set(BRANDS.DESCRIPTION, event.description)
            .set(BRANDS.STATUS, BrandStatus.ACTIVE.name)
            .set(BRANDS.CREATED_AT, at)
            .set(BRANDS.UPDATED_AT, at)
            .onDuplicateKeyIgnore()
            .execute()
    }

    @EventHandler
    fun on(
        event: BrandUpdatedEvent,
        @Timestamp timestamp: Instant,
    ) {
        dsl
            .update(BRANDS)
            .set(BRANDS.NAME, event.name)
            .set(BRANDS.DESCRIPTION, event.description)
            .set(BRANDS.UPDATED_AT, LocalDateTime.ofInstant(timestamp, ZoneOffset.UTC))
            .where(BRANDS.ID.eq(event.id))
            .execute()
    }

    @EventHandler
    fun on(
        event: BrandArchivedEvent,
        @Timestamp timestamp: Instant,
    ) {
        dsl
            .update(BRANDS)
            .set(BRANDS.STATUS, BrandStatus.ARCHIVED.name)
            .set(BRANDS.UPDATED_AT, LocalDateTime.ofInstant(timestamp, ZoneOffset.UTC))
            .where(BRANDS.ID.eq(event.id))
            .execute()
    }
}
