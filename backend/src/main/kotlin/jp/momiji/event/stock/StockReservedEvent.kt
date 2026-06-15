package jp.momiji.event.stock

import jp.momiji.event.MomijiEventTag
import org.axonframework.eventsourcing.annotation.EventTag
import org.axonframework.messaging.eventhandling.annotation.Event

/**
 * 在庫予約（注文開始で在庫を確保した）イベント。 product_id と order_id の **2 タグ**を持つ。
 * - product_id: 在庫の整合境界（oversell 防止のため product 単位で予約数を集計する）
 * - order_id: どの注文の予約か（後続の確保解放／在庫確定が order で相関できる）
 *
 * [reservedQuantity] は予約後の **予約数の絶対値**（StockReceivedEvent の onHandQuantity と同じ思想）。
 * read model の reserved 列を**冪等に上書き**できるよう絶対値を持つ。 [quantity] はこの注文の予約分（差分）。
 */
@Event(namespace = "momiji.stock", name = "StockReservedEvent")
data class StockReservedEvent(
    @EventTag(key = MomijiEventTag.PRODUCT_ID)
    val productId: String,
    @EventTag(key = MomijiEventTag.ORDER_ID)
    val orderId: String,
    // この注文で予約した個数（差分）。 確保解放／在庫確定で戻す量になる。
    val quantity: Int,
    // 予約後の予約数（絶対値）。 read model の reserved を冪等に set するため。
    val reservedQuantity: Int,
)
