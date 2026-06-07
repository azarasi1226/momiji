package jp.momiji.event.stock

import jp.momiji.event.MomijiEventTag
import org.axonframework.eventsourcing.annotation.EventTag
import org.axonframework.messaging.eventhandling.annotation.Event

/**
 * 在庫調整（販売以外の理由で在庫を増減した）イベント。
 */
@Event(namespace = "momiji.stock", name = "StockAdjustedEvent")
data class StockAdjustedEvent(
    @EventTag(key = MomijiEventTag.PRODUCT_ID)
    val productId: String,
    // 今回の調整数（符号付き差分）
    val adjustmentQuantity: Int,
    // 調整理由（StockAdjustmentReason の name）
    // Enumで持たせたかったけど、Enum名の変更がイベントの互換性に影響するため、文字列で持たせることにした
    val reason: String,
    // 調整後の物理在庫（絶対値）
    val onHandQuantity: Int,
)
