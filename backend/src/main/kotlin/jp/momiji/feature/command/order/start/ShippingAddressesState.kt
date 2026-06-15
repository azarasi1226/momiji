package jp.momiji.feature.command.order.start

import jp.momiji.event.MomijiEventTag
import jp.momiji.event.eventQualifiedName
import jp.momiji.event.order.OrderStartedEvent
import jp.momiji.event.user.ShippingAddressDeletedEvent
import jp.momiji.event.user.ShippingAddressRegisteredEvent
import jp.momiji.event.user.ShippingAddressUpdatedEvent
import org.axonframework.eventsourcing.annotation.EventCriteriaBuilder
import org.axonframework.eventsourcing.annotation.EventSourcingHandler
import org.axonframework.eventsourcing.annotation.reflection.EntityCreator
import org.axonframework.extension.spring.stereotype.EventSourced
import org.axonframework.messaging.eventstreaming.EventCriteria
import org.axonframework.messaging.eventstreaming.Tag

/**
 * user_id 境界の DCB State。 このユーザーが持つ配送先集合（所有権検証＋スナップショット用）。
 *
 * @InjectEntity(idProperty = "userId") で StartOrderCommand.userId から解決される。 他人の住所は
 * user_id 境界の外なのでここに現れない（所有権検証＝ membership で IDOR を構造的に塞ぐ）。
 */
@EventSourced(idType = String::class)
class ShippingAddressesState(
    private val byId: MutableMap<String, OrderStartedEvent.SnapshotShippingAddress>,
) {
    companion object {
        @JvmStatic
        @EventCriteriaBuilder
        private fun resolveCriteria(userId: String): EventCriteria =
            EventCriteria
                .havingTags(Tag.of(MomijiEventTag.USER_ID, userId))
                .andBeingOneOfTypes(
                    ShippingAddressRegisteredEvent::class.eventQualifiedName(),
                    ShippingAddressUpdatedEvent::class.eventQualifiedName(),
                    ShippingAddressDeletedEvent::class.eventQualifiedName(),
                )
    }

    @EntityCreator
    constructor() : this(mutableMapOf())

    fun find(shippingAddressId: String): OrderStartedEvent.SnapshotShippingAddress? = byId[shippingAddressId]

    @EventSourcingHandler
    fun evolve(event: ShippingAddressRegisteredEvent) {
        byId[event.shippingAddressId] = event.toSnapshot()
    }

    @EventSourcingHandler
    fun evolve(event: ShippingAddressUpdatedEvent) {
        byId[event.shippingAddressId] = event.toSnapshot()
    }

    @EventSourcingHandler
    fun evolve(event: ShippingAddressDeletedEvent) {
        byId.remove(event.shippingAddressId)
    }

    private fun ShippingAddressRegisteredEvent.toSnapshot() =
        OrderStartedEvent.SnapshotShippingAddress(
            name = name,
            phoneNumber = phoneNumber,
            postalCode = postalCode,
            prefecture = prefecture,
            city = city,
            streetAddress = streetAddress,
            building = building,
            deliveryNote = deliveryNote,
        )

    private fun ShippingAddressUpdatedEvent.toSnapshot() =
        OrderStartedEvent.SnapshotShippingAddress(
            name = name,
            phoneNumber = phoneNumber,
            postalCode = postalCode,
            prefecture = prefecture,
            city = city,
            streetAddress = streetAddress,
            building = building,
            deliveryNote = deliveryNote,
        )
}
