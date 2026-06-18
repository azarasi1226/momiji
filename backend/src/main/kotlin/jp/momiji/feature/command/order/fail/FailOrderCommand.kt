package jp.momiji.feature.command.order.fail

import jp.momiji.domain.order.OrderFailureReason
import jp.momiji.feature.command.CommandResult
import jp.momiji.feature.command.order.OrderProductIds
import kotlinx.coroutines.future.await
import org.axonframework.messaging.commandhandling.gateway.CommandGateway

/**
 * 注文を失敗させる補償コマンド。 在庫予約を解放し、 注文を FAILED にする。
 *
 * 整合境界は **order_id ＋ 全 product_id**:
 * - order_id（[jp.momiji.feature.command.order.OrderState]）
 * - product_id（×n, [jp.momiji.feature.command.order.ProductsState]）: 各商品の現在の予約数（解放後の絶対値を出すため）
 */
data class FailOrderCommand(
    val orderId: String,
    val productIds: List<String>,
    val reason: OrderFailureReason,
) {
    // ProductsState の id（@InjectEntity(idProperty = "orderProductIds")）。
    val orderProductIds: OrderProductIds
        get() = OrderProductIds(productIds)
}

object FailOrderCommandResult {
    // releasable でなければ no-op で success（冪等）。 解放まで行った場合も success。
    fun success() = CommandResult.success()
}

suspend fun CommandGateway.failOrder(command: FailOrderCommand): CommandResult = send(command, CommandResult::class.java).await()
