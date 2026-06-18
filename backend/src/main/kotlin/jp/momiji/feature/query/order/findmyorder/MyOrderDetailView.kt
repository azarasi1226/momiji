package jp.momiji.feature.query.order.findmyorder

import java.time.LocalDateTime

/** 自分の注文 1 件の詳細 read view（購入者の注文詳細画面用）。 注文時点スナップショットで自己完結する。 */
data class MyOrderDetailView(
    val orderId: String,
    // read model の status 文字列（= domain.order.OrderStatus の name）。 proto 変換は GrpcService 側。
    val status: String,
    val totalAmount: Long,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val shippingAddress: ShippingAddress,
    // 使用した保存カード。 未着手や後のカード削除で無い場合は null。
    val paymentMethod: PaymentMethod?,
    val items: List<Item>,
) {
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

    data class PaymentMethod(
        val id: String,
        val brand: String,
        val last4: String,
    )

    data class Item(
        val productId: String,
        val name: String,
        val unitPrice: Int,
        val quantity: Int,
        val imageUrl: String?,
    )
}
