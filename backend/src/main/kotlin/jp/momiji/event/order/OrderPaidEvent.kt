package jp.momiji.event.order

import jp.momiji.event.MomijiEventTag
import org.axonframework.eventsourcing.annotation.EventTag
import org.axonframework.messaging.eventhandling.annotation.Event

/**
 * 決済成功イベント（PAYMENT_PENDING → PAID）.
 */
@Event(namespace = "momiji.order", name = "OrderPaidEvent")
data class OrderPaidEvent(
    @EventTag(key = MomijiEventTag.ORDER_ID)
    val orderId: String,
)
