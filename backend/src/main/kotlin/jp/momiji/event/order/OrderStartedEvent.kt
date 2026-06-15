package jp.momiji.event.order

import jp.momiji.event.MomijiEventTag
import org.axonframework.eventsourcing.annotation.EventTag
import org.axonframework.messaging.eventhandling.annotation.Event

/**
 * 注文開始　イベント。
 *
 * 在庫の予約そのものは [jp.momiji.event.stock.StockReservedEvent]（product_id 単位）が表す。
 * このイベントは「どの user の、 どこに送る、 どの商品×個数の注文が始まったか」のスナップショットを持つ。
 *
 * **配送先は ID 参照でなくスナップショット**にする: 注文後に配送先が編集・削除されても、
 * 過去の注文の宛先が変わったり壊れたりしないように、 注文時点の住所を焼き込む。
 */
@Event(namespace = "momiji.order", name = "OrderStartedEvent")
data class OrderStartedEvent(
    @EventTag(key = MomijiEventTag.ORDER_ID)
    val orderId: String,
    @EventTag(key = MomijiEventTag.USER_ID)
    val userId: String,
    val shippingAddress: SnapshotShippingAddress,
    val items: List<SnapshotItem>,
) {
    data class SnapshotShippingAddress(
        val name: String,
        val phoneNumber: String,
        val postalCode: String,
        val prefecture: String,
        val city: String,
        val streetAddress: String,
        val building: String,
        val deliveryNote: String,
    )

    data class SnapshotItem(
        val productId: String,
        val name: String,
        val unitPrice: Int,
        val quantity: Int,
        val imageUrl: String? = null,
    )
}
