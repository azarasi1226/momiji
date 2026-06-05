package jp.momiji.projection.brand

import iss.jooq.generated.tables.references.BRANDS
import jp.momiji.event.brand.BrandCreatedEvent
import jp.momiji.event.brand.BrandDeletedEvent
import jp.momiji.event.brand.BrandUpdatedEvent
import jp.momiji.feature.InitialPosition
import org.axonframework.messaging.eventhandling.annotation.EventHandler
import org.axonframework.messaging.eventhandling.annotation.Timestamp
import org.jooq.DSLContext
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * brands read model を更新する projector。
 *
 * read model なので **PooledStreaming + [InitialPosition.FIRST]**（新規デプロイ時に先頭から
 * 全再生して read model を構築する）。 再生に対して安全であるよう、 全ハンドラを冪等にしている:
 * - Created: `onDuplicateKeyIgnore`（再生で二重 insert されても無害）
 * - Updated / Deleted: `where` 指定の update / delete（対象不在でも no-op）
 * - 日時は [Timestamp]（イベント発生時刻）由来。 `LocalDateTime.now()` は再生で値が変わるため使わない（ADR 0008）。
 */
@Component
class BrandTableProjector(
    private val dsl: DSLContext,
) {
    @EventHandler
    fun on(
        event: BrandCreatedEvent,
        @Timestamp timestamp: Instant,
    ) {
        val at = LocalDateTime.ofInstant(timestamp, ZoneId.systemDefault())
        dsl
            .insertInto(BRANDS)
            .set(BRANDS.ID, event.id)
            .set(BRANDS.NAME, event.name)
            .set(BRANDS.DESCRIPTION, event.description)
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
            .set(BRANDS.UPDATED_AT, LocalDateTime.ofInstant(timestamp, ZoneId.systemDefault()))
            .where(BRANDS.ID.eq(event.id))
            .execute()
    }

    @EventHandler
    fun on(event: BrandDeletedEvent) {
        dsl
            .deleteFrom(BRANDS)
            .where(BRANDS.ID.eq(event.id))
            .execute()
    }
}
