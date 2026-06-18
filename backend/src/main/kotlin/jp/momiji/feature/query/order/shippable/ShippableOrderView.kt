package jp.momiji.feature.query.order.shippable

import java.time.LocalDateTime

/** 発送待ち注文の read view（admin 発送管理用）。 */
data class ShippableOrderView(
    val orderId: String,
    val shippingAddress: ShippingAddress,
    val totalAmount: Long,
    val createdAt: LocalDateTime,
    val items: List<Item>,
) {
    /** 注文時点スナップショットの配送先（admin が荷造り・発送に使う）。 */
    data class ShippingAddress(
        val recipientName: String,
        val phoneNumber: String,
        val postalCode: String,
        val prefecture: String,
        val city: String,
        val streetAddress: String,
        val building: String,
        val deliveryNote: String,
    )

    data class Item(
        val name: String,
        val quantity: Int,
    )
}
