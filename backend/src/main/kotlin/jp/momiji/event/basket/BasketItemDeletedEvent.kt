package jp.momiji.event.basket

import jp.momiji.event.MomijiEventTag
import org.axonframework.eventsourcing.annotation.EventTag
import org.axonframework.messaging.eventhandling.annotation.Event

/** 買い物かごから 1 商品を取り除いたイベント。 */
@Event(namespace = "momiji.basket", name = "BasketItemDeletedEvent")
data class BasketItemDeletedEvent(
    @EventTag(key = MomijiEventTag.USER_ID)
    val userId: String,
    @EventTag(key = MomijiEventTag.PRODUCT_ID)
    val productId: String,
)
