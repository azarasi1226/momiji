package jp.momiji.feature.command.order.start

import jp.momiji.domain.BusinessError
import jp.momiji.feature.command.CommandResult
import jp.momiji.feature.command.order.OrderProductIds
import kotlinx.coroutines.future.await
import org.axonframework.messaging.commandhandling.gateway.CommandGateway

/**
 * 注文を開始するコマンド。 カート（user の baskets）の内容を [items] として受け取り、 配送先 [shippingAddressId] の
 * 選択を添えて、 **order と 各 product の在庫を 1 整合境界で atomic に**確保する。
 *
 * 整合境界（DCB のターゲット）は **order_id ＋ user_id ＋ 全 product_id**:
 * - order_id: order が既に開始済みか（冪等）
 * - user_id: ユーザーの配送先集合（**所有権検証**: 他人の住所を使わせない）
 * - product_id（×n）: 各商品の ACTIVE 判定・在庫（oversell 防止）
 *
 * これで全商品の予約・所有権検証を**全部成功 or 全部失敗**で確定する。 [id] はサーバ採番（gRPC 層で ULID）。
 * 決済カードは決済フェーズ（決済準備）で受け取る（client 主導で再試行できる段階なので、 注文開始には含めない）。
 */
data class StartOrderCommand(
    val id: String,
    val userId: String,
    val shippingAddressId: String,
    // クライアントが見ていた商品合計金額（円）。 server の権威価格で計算した合計と一致するか検証する。
    val expectedTotalAmount: Long,
    val items: List<Item>,
) {
    data class Item(
        val productId: String,
        val quantity: Int,
    )

    // 各 DCB エンティティの id は @InjectEntity(idProperty = ...) で個別解決する:
    // - OrderState          → "id"（order_id）
    // - ShippingAddressesState → "userId"
    // - ProductsState       → "productIds"（下の派生プロパティ）
    val productIds: OrderProductIds
        get() = OrderProductIds(items.map { it.productId })
}

object StartOrderCommandResult {
    fun success() = CommandResult.success()

    fun emptyOrder() = CommandResult.fail(BusinessError("注文する商品がありません"))

    fun shippingAddressNotFound() = CommandResult.fail(BusinessError("配送先が見つかりません"))

    fun amountMismatch() = CommandResult.fail(BusinessError("金額が変わっています。 カートをご確認のうえ、 もう一度お試しください"))

    fun productNotFound(productIds: List<String>) = CommandResult.fail(BusinessError("購入できない商品があります: $productIds"))

    fun outOfStock(productIds: List<String>) = CommandResult.fail(BusinessError("在庫が不足している商品があります: $productIds"))
}

suspend fun CommandGateway.startOrder(command: StartOrderCommand): CommandResult = send(command, CommandResult::class.java).await()
