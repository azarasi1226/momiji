package jp.momiji.event.order

import jp.momiji.event.MomijiEventTag
import org.axonframework.eventsourcing.annotation.EventTag
import org.axonframework.messaging.eventhandling.annotation.Event

/**
 * 決済準備イベント（STARTED → PAYMENT_PENDING）。
 * その事実（pi_ ＋ 使うカード pm_）を記録する。
 *
 * これにより注文は「決済着手済み」になり、 **予約タイムアウトの時計がリセット**される（3DS に十分な時間を確保）。
 * [paymentIntentId] は後続の支払い結果 webhook 相関・期限切れ後課金の返金に使う。
 */
@Event(namespace = "momiji.order", name = "OrderPaymentPreparedEvent")
data class OrderPaymentPreparedEvent(
    @EventTag(key = MomijiEventTag.ORDER_ID)
    val orderId: String,
    // 決済に使うカード id
    @EventTag(key = MomijiEventTag.PAYMENT_METHOD_ID)
    val paymentMethodId: String,
    // 　返金時に使う決済 id
    val paymentIntentId: String,
)
