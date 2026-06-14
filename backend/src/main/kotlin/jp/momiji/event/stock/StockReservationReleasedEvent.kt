package jp.momiji.event.stock

import jp.momiji.event.MomijiEventTag
import org.axonframework.eventsourcing.annotation.EventTag
import org.axonframework.messaging.eventhandling.annotation.Event

/**
 * 在庫予約の解放（注文の失敗・期限切れで確保していた在庫を戻した）イベント。 [StockReservedEvent] の逆。
 * product_id と order_id の **2 タグ**を持つ。
 *
 * [reservedQuantity] は解放後の **予約数の絶対値**（read model の reserved 列を冪等に上書きするため）。
 * [quantity] はこの注文ぶんの解放個数（差分）。
 */
@Event(namespace = "momiji.stock", name = "StockReservationReleasedEvent")
data class StockReservationReleasedEvent(
    @EventTag(key = MomijiEventTag.PRODUCT_ID)
    val productId: String,
    @EventTag(key = MomijiEventTag.ORDER_ID)
    val orderId: String,
    // この注文で解放した個数（差分）。
    val quantity: Int,
    // 解放後の予約数（絶対値）。 read model の reserved を冪等に set するため。
    val reservedQuantity: Int,
)
