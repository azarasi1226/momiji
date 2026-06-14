package jp.momiji.feature.command.order.expire

import jp.momiji.feature.command.CommandResult
import jp.momiji.feature.command.order.start.OrderProductIds
import kotlinx.coroutines.future.await
import org.axonframework.messaging.commandhandling.gateway.CommandGateway

/**
 * 予約タイムアウトで注文を失効させるコマンド。 STARTED の注文の在庫予約を解放し、 注文を FAILED にする。
 * sweeper（[OrderExpirySweeper]）が期限切れ注文に対して撃つ。 将来の支払い失敗の補償と同じ解放へ合流する。
 *
 * 整合境界は **order_id ＋ 全 product_id**:
 * - order_id（[OrderState]）: STARTED か（冪等ガード）＋ 解放対象の予約スナップショット
 * - product_id（×n, [ProductsState]）: 各商品の現在の予約数（解放後の絶対値を出すため）
 *
 * [productIds] は order_items（read model）由来。 ProductsState の境界を組むために sweeper が渡す。
 * 解放する個数は OrderState の予約スナップショット（権威）から取る。
 */
data class ExpireOrderCommand(
    val orderId: String,
    val productIds: List<String>,
) {
    // ProductsState の id（@InjectEntity(idProperty = "orderProductIds")）。
    val orderProductIds: OrderProductIds
        get() = OrderProductIds(productIds)
}

object ExpireOrderCommandResult {
    // STARTED でなければ no-op で success（冪等）。 解放まで行った場合も success。
    fun success() = CommandResult.success()
}

suspend fun CommandGateway.expireOrder(command: ExpireOrderCommand): CommandResult = send(command, CommandResult::class.java).await()
