package jp.momiji.event.user

import jp.momiji.event.MomijiEventTag
import org.axonframework.eventsourcing.annotation.EventTag
import org.axonframework.messaging.eventhandling.annotation.Event

/**
 * 配送先を登録したイベント。 「住所が増えた」という事実だけを表す。
 *
 * 配送先はユーザー本人の住所とは限らないため、 受取人の氏名とドライバー連絡用の電話番号を持つ。
 *
 * default かどうかはこのイベントは関知しない —— default の付与・移動はすべて
 * [DefaultShippingAddressChangedEvent] が担う（初回登録時はコマンドが 2 イベントを一括追記する）。
 * 役割を分けることで、 default の変化経路が 1 イベント型に一本化される。
 */
@Event(namespace = "momiji.user", name = "ShippingAddressRegisteredEvent")
data class ShippingAddressRegisteredEvent(
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
