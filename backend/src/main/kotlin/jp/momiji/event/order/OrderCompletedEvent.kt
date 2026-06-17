package jp.momiji.event.order

import jp.momiji.event.MomijiEventTag
import org.axonframework.eventsourcing.annotation.EventTag
import org.axonframework.messaging.eventhandling.annotation.Event

/**
 * 注文完了イベント（SHIPPED → COMPLETED）
 */
@Event(namespace = "momiji.order", name = "OrderCompletedEvent")
data class OrderCompletedEvent(
    @EventTag(key = MomijiEventTag.ORDER_ID)
    val orderId: String,
)
