package jp.momiji.feature.command.order.start

import jp.momiji.event.MomijiEventTag
import jp.momiji.event.eventQualifiedName
import jp.momiji.event.order.OrderStartedEvent
import org.axonframework.eventsourcing.annotation.EventCriteriaBuilder
import org.axonframework.eventsourcing.annotation.EventSourcingHandler
import org.axonframework.eventsourcing.annotation.reflection.EntityCreator
import org.axonframework.extension.spring.stereotype.EventSourced
import org.axonframework.messaging.eventstreaming.EventCriteria
import org.axonframework.messaging.eventstreaming.Tag

/**
 * order_id 境界の DCB State。 この order が既に開始済みか（冪等判定）だけを見る。
 *
 * @InjectEntity(idProperty = "id") で StartOrderCommand.id（order_id）から解決される。
 */
@EventSourced(idType = String::class)
class OrderState(
    var started: Boolean,
) {
    companion object {
        @JvmStatic
        @EventCriteriaBuilder
        private fun resolveCriteria(orderId: String): EventCriteria =
            EventCriteria
                .havingTags(Tag.of(MomijiEventTag.ORDER_ID, orderId))
                .andBeingOneOfTypes(
                    OrderStartedEvent::class.eventQualifiedName(),
                )
    }

    @EntityCreator
    constructor() : this(started = false)

    @EventSourcingHandler
    fun evolve(event: OrderStartedEvent) {
        started = true
    }
}
