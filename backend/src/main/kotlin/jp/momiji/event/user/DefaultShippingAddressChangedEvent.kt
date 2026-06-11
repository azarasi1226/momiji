package jp.momiji.event.user

import jp.momiji.event.MomijiEventTag
import org.axonframework.eventsourcing.annotation.EventTag
import org.axonframework.messaging.eventhandling.annotation.Event

/**
 * デフォルト配送先を変更したイベント。 指す配送先を新しいデフォルトにする
 * （projection 側で同一ユーザーの他の配送先の is_default を 0 に倒す）。
 *
 * 発行元は「登録時の makeDefault 指定」と、 将来の「デフォルト変更ユースケース」「default 削除時の昇格」。
 */
@Event(namespace = "momiji.user", name = "DefaultShippingAddressChangedEvent")
data class DefaultShippingAddressChangedEvent(
    @EventTag(key = MomijiEventTag.USER_ID)
    val userId: String,
    @EventTag(key = MomijiEventTag.SHIPPING_ADDRESS_ID)
    val shippingAddressId: String,
)
