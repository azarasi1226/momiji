package jp.momiji.feature.command.order.start

import jp.momiji.domain.order.OrderStatus
import jp.momiji.event.MomijiEventTag
import jp.momiji.event.eventQualifiedName
import jp.momiji.event.order.OrderFailedEvent
import jp.momiji.event.order.OrderStartedEvent
import org.axonframework.eventsourcing.annotation.EventCriteriaBuilder
import org.axonframework.eventsourcing.annotation.EventSourcingHandler
import org.axonframework.eventsourcing.annotation.reflection.EntityCreator
import org.axonframework.extension.spring.stereotype.EventSourced
import org.axonframework.messaging.eventstreaming.EventCriteria
import org.axonframework.messaging.eventstreaming.Tag

/**
 * order_id 境界の DCB State。 注文のライフサイクル状態（[status]）と、 予約した商品（[reservedItems]）を見る。
 *
 * - StartOrder: 既に開始済みか（[status] != null）の冪等判定に使う。
 * - ExpireOrder: STARTED のときだけ失効させ、 [reservedItems] を解放対象にする。
 *
 * @InjectEntity(idProperty = ...) で order_id を渡して解決される（StartOrder は "id"、 ExpireOrder は "orderId"）。
 */
@EventSourced(idType = String::class)
class OrderState(
    var status: OrderStatus?,
    // 予約された商品（OrderStarted のスナップショット由来）。 失効時の解放対象。
    val reservedItems: MutableList<ReservedItem>,
) {
    data class ReservedItem(
        val productId: String,
        val quantity: Int,
    )

    companion object {
        @JvmStatic
        @EventCriteriaBuilder
        private fun resolveCriteria(orderId: String): EventCriteria =
            EventCriteria
                .havingTags(Tag.of(MomijiEventTag.ORDER_ID, orderId))
                .andBeingOneOfTypes(
                    OrderStartedEvent::class.eventQualifiedName(),
                    OrderFailedEvent::class.eventQualifiedName(),
                )
    }

    @EntityCreator
    constructor() : this(status = null, reservedItems = mutableListOf())

    /** まだ注文が開始されていない（この order_id のイベントが無い）。 StartOrder の冪等判定に使う。 */
    val notStarted: Boolean get() = status == null

    /** STARTED（予約済み・未確定）。 ExpireOrder はこの状態のときだけ解放する。 */
    val isStarted: Boolean get() = status == OrderStatus.STARTED

    @EventSourcingHandler
    fun evolve(event: OrderStartedEvent) {
        status = OrderStatus.STARTED
        reservedItems.addAll(event.items.map { ReservedItem(it.productId, it.quantity) })
    }

    @EventSourcingHandler
    fun evolve(event: OrderFailedEvent) {
        status = OrderStatus.FAILED
    }
}
