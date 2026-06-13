package jp.momiji.event.user

import jp.momiji.event.MomijiEventTag
import org.axonframework.eventsourcing.annotation.EventTag
import org.axonframework.messaging.eventhandling.annotation.Event

/**
 * 配送先を削除したイベント。
 *
 * 削除したのが default だった場合の昇格は、 コマンドが [DefaultShippingAddressChangedEvent] を
 * 同時追記することで表現する（このイベント自体は default を関知しない）。
 */
@Event(namespace = "momiji.user", name = "ShippingAddressDeletedEvent")
data class ShippingAddressDeletedEvent(
    @EventTag(key = MomijiEventTag.USER_ID)
    val userId: String,
    @EventTag(key = MomijiEventTag.SHIPPING_ADDRESS_ID)
    val shippingAddressId: String,
)
