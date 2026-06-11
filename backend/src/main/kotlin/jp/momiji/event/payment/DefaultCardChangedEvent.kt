package jp.momiji.event.payment

import jp.momiji.event.MomijiEventTag
import org.axonframework.eventsourcing.annotation.EventTag
import org.axonframework.messaging.eventhandling.annotation.Event

/**
 * デフォルトカードを変更したイベント。
 *
 * 指す pm_ を新しいデフォルトにする。 projection 側で同一ユーザーの他カードの is_default を 0 に倒す。
 */
@Event(namespace = "momiji.payment", name = "DefaultCardChangedEvent")
data class DefaultCardChangedEvent(
    @EventTag(key = MomijiEventTag.USER_ID)
    val userId: String,
    @EventTag(key = MomijiEventTag.PAYMENT_METHOD_ID)
    val paymentMethodId: String,
)
