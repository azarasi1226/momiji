package jp.momiji.event.order

import jp.momiji.event.MomijiEventTag
import org.axonframework.eventsourcing.annotation.EventTag
import org.axonframework.messaging.eventhandling.annotation.Event

/**
 * 注文失敗イベント（オーダーのライフサイクル FAILED）。 確保していた在庫の解放（[jp.momiji.event.stock.StockReservationReleasedEvent]）
 * と同じコマンドで同梱発行される。
 *
 * [reason] は失敗理由（[jp.momiji.domain.order.OrderFailureReason] の name）。 期限切れと（将来の）支払い失敗が
 * この 1 イベントに合流する。
 */
@Event(namespace = "momiji.order", name = "OrderFailedEvent")
data class OrderFailedEvent(
    @EventTag(key = MomijiEventTag.ORDER_ID)
    val orderId: String,
    val reason: String,
)
