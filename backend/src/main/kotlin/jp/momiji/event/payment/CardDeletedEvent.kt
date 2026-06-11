package jp.momiji.event.payment

import jp.momiji.event.MomijiEventTag
import org.axonframework.eventsourcing.annotation.EventTag
import org.axonframework.messaging.eventhandling.annotation.Event

/**
 * 登録済みカード（pm_）を削除したイベント。
 *
 * このイベントは「momiji 側で削除した事実」。 Stripe 側の DetachPaymentMethod は
 * 後続の冪等な外部副作用ハンドラ（[jp.momiji.feature.command.payment.deletecard.CardDetacher]）が行う。
 */
@Event(namespace = "momiji.payment", name = "CardDeletedEvent")
data class CardDeletedEvent(
    @EventTag(key = MomijiEventTag.USER_ID)
    val userId: String,
    @EventTag(key = MomijiEventTag.PAYMENT_METHOD_ID)
    val paymentMethodId: String,
)
