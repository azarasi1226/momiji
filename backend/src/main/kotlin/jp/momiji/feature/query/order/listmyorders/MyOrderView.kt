package jp.momiji.feature.query.order.listmyorders

import java.time.LocalDateTime

/** 自分の注文一覧の read view（購入者の注文履歴画面用）。 注文時点スナップショット（商品名・単価）で自己完結する。 */
data class MyOrderView(
    val orderId: String,
    // read model の status 文字列（= domain.order.OrderStatus の name）。 proto 変換は GrpcService 側で行う。
    val status: String,
    // 注文時点スナップショットの合計金額（単価 × 個数の総和）。
    val totalAmount: Long,
    val createdAt: LocalDateTime,
    val items: List<Item>,
) {
    data class Item(
        val productId: String,
        val name: String,
        val unitPrice: Int,
        val quantity: Int,
        val imageUrl: String?,
    )
}
