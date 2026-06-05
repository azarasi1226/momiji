package jp.momiji.event.brand

import jp.momiji.event.MomijiEventTag
import org.axonframework.eventsourcing.annotation.EventTag
import org.axonframework.messaging.eventhandling.annotation.Event

@Event(namespace = "momiji.brand", name = "BrandUpdatedEvent")
data class BrandUpdatedEvent(
    @EventTag(key = MomijiEventTag.BRAND_ID)
    val id: String,
    val name: String,
    val description: String,
)
