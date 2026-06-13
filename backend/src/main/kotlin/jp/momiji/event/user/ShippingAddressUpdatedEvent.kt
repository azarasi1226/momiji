package jp.momiji.event.user

import jp.momiji.event.MomijiEventTag
import org.axonframework.eventsourcing.annotation.EventTag
import org.axonframework.messaging.eventhandling.annotation.Event

/**
 * 配送先を編集したイベント。 編集後の全フィールドのスナップショットを持つ。
 *
 * default かどうかは関知しない（[DefaultShippingAddressChangedEvent] の責務）。
 */
@Event(namespace = "momiji.user", name = "ShippingAddressUpdatedEvent")
data class ShippingAddressUpdatedEvent(
    @EventTag(key = MomijiEventTag.USER_ID)
    val userId: String,
    @EventTag(key = MomijiEventTag.SHIPPING_ADDRESS_ID)
    val shippingAddressId: String,
    val name: String,
    val phoneNumber: String,
    val postalCode: String,
    val prefecture: String,
    val city: String,
    val streetAddress: String,
    val building: String,
    val deliveryNote: String,
)
