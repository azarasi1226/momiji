package jp.momiji.event.product

import jp.momiji.event.MomijiEventTag
import org.axonframework.eventsourcing.annotation.EventTag
import org.axonframework.messaging.eventhandling.annotation.Event

@Event(namespace = "momiji.product", name = "ProductCreatedEvent")
data class ProductCreatedEvent(
    @EventTag(key = MomijiEventTag.PRODUCT_ID)
    val id: String,
    val brandId: String,
    val name: String,
    val description: String,
    val imageUrl: String?,
    val price: Int,
)
