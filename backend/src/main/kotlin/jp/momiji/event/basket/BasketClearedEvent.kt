package jp.momiji.event.basket

import jp.momiji.event.MomijiEventTag
import org.axonframework.eventsourcing.annotation.EventTag
import org.axonframework.messaging.eventhandling.annotation.Event

/** 買い物かごを空にしたイベント。 */
@Event(namespace = "momiji.basket", name = "BasketClearedEvent")
data class BasketClearedEvent(
    @EventTag(key = MomijiEventTag.USER_ID)
    val userId: String,
)
