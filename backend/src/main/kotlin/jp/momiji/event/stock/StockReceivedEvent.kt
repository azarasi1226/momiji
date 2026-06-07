package jp.momiji.event.stock

import jp.momiji.event.MomijiEventTag
import org.axonframework.eventsourcing.annotation.EventTag
import org.axonframework.messaging.eventhandling.annotation.Event

/**
 * 入庫（在庫を増やした）イベント。
 */
@Event(namespace = "momiji.stock", name = "StockReceivedEvent")
data class StockReceivedEvent(
    @EventTag(key = MomijiEventTag.PRODUCT_ID)
    val productId: String,
    // 今回の入庫数（差分）
    val receivedQuantity: Int,
    // 入庫後の物理在庫（絶対値）
    val onHandQuantity: Int,
)
