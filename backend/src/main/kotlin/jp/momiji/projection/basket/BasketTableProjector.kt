package jp.momiji.projection.basket

import iss.jooq.generated.tables.references.BASKETS
import jp.momiji.event.basket.BasketClearedEvent
import jp.momiji.event.basket.BasketItemDeletedEvent
import jp.momiji.event.basket.BasketItemSetEvent
import jp.momiji.event.product.ProductDiscontinuedEvent
import jp.momiji.event.user.UserDeletedEvent
import org.axonframework.messaging.eventhandling.annotation.EventHandler
import org.axonframework.messaging.eventhandling.annotation.Timestamp
import org.jooq.DSLContext
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

@Component
class BasketTableProjector(
    private val dsl: DSLContext,
) {
    @EventHandler
    fun on(
        event: BasketItemSetEvent,
        @Timestamp timestamp: Instant,
    ) {
        val at = LocalDateTime.ofInstant(timestamp, ZoneOffset.UTC)
        dsl
            .insertInto(BASKETS)
            .set(BASKETS.USER_ID, event.userId)
            .set(BASKETS.PRODUCT_ID, event.productId)
            .set(BASKETS.ITEM_QUANTITY, event.itemQuantity)
            .set(BASKETS.ADDED_AT, at)
            .onDuplicateKeyUpdate()
            .set(BASKETS.ITEM_QUANTITY, event.itemQuantity)
            .set(BASKETS.ADDED_AT, at)
            .execute()
    }

    @EventHandler
    fun on(event: BasketItemDeletedEvent) {
        dsl
            .deleteFrom(BASKETS)
            .where(BASKETS.USER_ID.eq(event.userId))
            .and(BASKETS.PRODUCT_ID.eq(event.productId))
            .execute()
    }

    @EventHandler
    fun on(event: BasketClearedEvent) {
        dsl
            .deleteFrom(BASKETS)
            .where(BASKETS.USER_ID.eq(event.userId))
            .execute()
    }

    // 商品が生産終了したら、その商品を含む全ユーザーのカゴから取り除く（カート掃除）。
    @EventHandler
    fun on(event: ProductDiscontinuedEvent) {
        dsl
            .deleteFrom(BASKETS)
            .where(BASKETS.PRODUCT_ID.eq(event.id))
            .execute()
    }

    // ユーザーが削除されたら、そのユーザーのカゴ行を掃除する（参照されない死にデータを残さない）。
    @EventHandler
    fun on(event: UserDeletedEvent) {
        dsl
            .deleteFrom(BASKETS)
            .where(BASKETS.USER_ID.eq(event.id))
            .execute()
    }
}
