package jp.momiji.event.order

import jp.momiji.event.MomijiEventTag
import org.axonframework.eventsourcing.annotation.EventTag
import org.axonframework.messaging.eventhandling.annotation.Event

/**
 * 発送手続きイベント（PAID → SHIPPED)
 */
@Event(namespace = "momiji.order", name = "OrderShippedEvent")
data class OrderShippedEvent(
    @EventTag(key = MomijiEventTag.ORDER_ID)
    val orderId: String,
)
