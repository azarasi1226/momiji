package jp.momiji.event.brand

import jp.momiji.event.MomijiEventTag
import org.axonframework.eventsourcing.annotation.EventTag
import org.axonframework.messaging.eventhandling.annotation.Event

@Event(namespace = "momiji.brand", name = "BrandCreatedEvent")
data class BrandCreatedEvent(
    @EventTag(key = MomijiEventTag.BRAND_ID)
    val id: String,
    val name: String,
    val description: String,
)
