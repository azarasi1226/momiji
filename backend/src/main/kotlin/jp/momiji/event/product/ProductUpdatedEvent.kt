package jp.momiji.event.product

import jp.momiji.event.MomijiEventTag
import org.axonframework.eventsourcing.annotation.EventTag
import org.axonframework.messaging.eventhandling.annotation.Event

/**
 * 商品更新イベント。 在庫数 (quantity) とブランド (brandId) は更新対象外なので持たない
 * （在庫変更は将来の専用コマンド、 ブランド付け替えは非対応）。
 */
@Event(namespace = "momiji.product", name = "ProductUpdatedEvent")
data class ProductUpdatedEvent(
    @EventTag(key = MomijiEventTag.PRODUCT_ID)
    val id: String,
    val name: String,
    val description: String,
    val imageUrl: String?,
    val price: Int,
)
