package jp.momiji.event.product

import jp.momiji.event.MomijiEventTag
import org.axonframework.eventsourcing.annotation.EventTag
import org.axonframework.messaging.eventhandling.annotation.Event

/**
 * 商品生産終了（廃番）イベント。 ハード削除の代わり（ライフサイクル: ACTIVE → DISCONTINUED）。
 * read model は行を消さず status を DISCONTINUED に更新する。
 */
@Event(namespace = "momiji.product", name = "ProductDiscontinuedEvent")
data class ProductDiscontinuedEvent(
    @EventTag(key = MomijiEventTag.PRODUCT_ID)
    val id: String,
)
