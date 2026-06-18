package jp.momiji.event.stock

import jp.momiji.event.MomijiEventTag
import org.axonframework.eventsourcing.annotation.EventTag
import org.axonframework.messaging.eventhandling.annotation.Event

/**
 * 在庫予約の引き当て確定（注文完了で予約していた在庫を出荷＝確定した）イベント。 product_id と order_id の **2 タグ**を持つ。
 *
 * 解放（[StockReservationReleasedEvent]＝available に戻す・onHand 不変）と違い、 **出荷された分 onHand も減る**
 * （reserved も減るので available は不変）。 注文完了（COMPLETED＝在庫確定済み）で発行される。
 *
 * [onHandQuantity] / [reservedQuantity] は確定後の**絶対値**（read model を冪等に上書きするため。 既存の在庫イベントと同じ思想）。
 * [quantity] はこの注文ぶんの確定個数（差分）。
 */
@Event(namespace = "momiji.stock", name = "StockReservationCommittedEvent")
data class StockReservationCommittedEvent(
    @EventTag(key = MomijiEventTag.PRODUCT_ID)
    val productId: String,
    @EventTag(key = MomijiEventTag.ORDER_ID)
    val orderId: String,
    // この注文で確定（出荷）した個数（差分）。
    val quantity: Int,
    // 確定後の在庫数（絶対値）。 出荷した分 onHand が減る。
    val onHandQuantity: Int,
    // 確定後の予約数（絶対値）。
    val reservedQuantity: Int,
)
