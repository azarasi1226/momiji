package jp.momiji.event.order

import jp.momiji.event.MomijiEventTag
import org.axonframework.eventsourcing.annotation.EventTag
import org.axonframework.messaging.eventhandling.annotation.Event

/**
 * 注文キャンセルイベント（オーダーのライフサイクル CANCELLED）。 ユーザー起点・発送前のキャンセル。
 * 確保していた在庫の解放（[jp.momiji.event.stock.StockReservationReleasedEvent]）と同じコマンドで同梱発行される。
 *
 * 失敗（[OrderFailedEvent]）とは区別する別イベント: 失敗は時間切れ/決済失敗の補償、 こちらはユーザー意思の取消。
 *
 * [reason] はキャンセル理由（[jp.momiji.domain.order.OrderCancellationReason] の name）。
 * [refundPaymentIntentId] は **課金確定済み（PAID）** のときだけ載る返金対象の pi_。 未課金（STARTED/PAYMENT_PENDING）は null。
 * 返金そのものは外部副作用なので、 このイベントを拾う [jp.momiji.feature.command.order.cancel.OrderRefunder]（非同期）が行う。
 */
@Event(namespace = "momiji.order", name = "OrderCancelledEvent")
data class OrderCancelledEvent(
    @EventTag(key = MomijiEventTag.ORDER_ID)
    val orderId: String,
    val reason: String,
    val refundPaymentIntentId: String? = null,
)
