package jp.momiji.event.user

import jp.momiji.event.MomijiEventTag
import org.axonframework.eventsourcing.annotation.EventTag
import org.axonframework.messaging.eventhandling.annotation.Event

/**
 * 配送先を登録したイベント。
 *
 * 配送先はユーザー本人の住所とは限らないため、 受取人の氏名とドライバー連絡用の電話番号を持つ。
 * [default] はそのユーザーの初回配送先（または default 不在の自己修復時）のとき true。
 *
 * NOTE: フィールド名を `isDefault` にすると Jackson が `is` を剥がして永続化キーがズレる
 * （Kotlin boolean の `is` プレフィックス問題。 CardRegisteredEvent と同じ）ため `default` で固定する。
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
    val default: Boolean,
)
