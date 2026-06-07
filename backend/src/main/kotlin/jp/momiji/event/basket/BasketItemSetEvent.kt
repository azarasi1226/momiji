package jp.momiji.event.basket

import jp.momiji.event.MomijiEventTag
import org.axonframework.eventsourcing.annotation.EventTag
import org.axonframework.messaging.eventhandling.annotation.Event

/**
 * 買い物かごに商品をセット（追加 or 個数変更）したイベント。
 *
 * カゴは**ユーザー単位**なので user_id タグを持つ（カゴ集約 = ユーザーのカゴ）。 商品参照のため product_id タグも持つ
 * （生産終了時に product_id でカゴから除去する等に使う）。 [itemQuantity] は加算でなく**設定値（絶対値）**。
 */
@Event(namespace = "momiji.basket", name = "BasketItemSetEvent")
data class BasketItemSetEvent(
    @EventTag(key = MomijiEventTag.USER_ID)
    val userId: String,
    @EventTag(key = MomijiEventTag.PRODUCT_ID)
    val productId: String,
    val itemQuantity: Int,
)
