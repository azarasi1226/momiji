package jp.momiji.feature.command.order.cancel

import jp.momiji.domain.BusinessError
import jp.momiji.domain.order.OrderCancellationReason
import jp.momiji.feature.command.CommandResult
import jp.momiji.feature.command.order.OrderProductIds
import kotlinx.coroutines.future.await
import org.axonframework.messaging.commandhandling.gateway.CommandGateway

/**
 * 注文をキャンセルするコマンド（ユーザー起点・発送前）。 在庫予約を解放し、 注文を CANCELLED にする。
 * 課金確定済み（PAID）なら返金対象の pi_ を [jp.momiji.event.order.OrderCancelledEvent] に載せ、 非同期に返金する。
 *
 * 整合境界は **order_id ＋ 全 product_id**（FailOrder と同じ）:
 * - order_id（[jp.momiji.feature.command.order.OrderState]）
 * - product_id（×n, [jp.momiji.feature.command.order.ProductsState]）: 解放後の予約数（絶対値）を出すため
 */
data class CancelOrderCommand(
    val orderId: String,
    val productIds: List<String>,
    val reason: OrderCancellationReason,
) {
    // ProductsState の id（@InjectEntity(idProperty = "orderProductIds")）。
    val orderProductIds: OrderProductIds
        get() = OrderProductIds(productIds)
}

object CancelOrderCommandResult {
    // キャンセル成立、 または既に終端（CANCELLED/FAILED）で no-op の冪等成功。
    fun success() = CommandResult.success()

    // 既に発送済み以降（SHIPPED/COMPLETED）でキャンセル不可。
    fun alreadyShipped() = CommandResult.fail(BusinessError("発送済みのためキャンセルできません"))
}

suspend fun CommandGateway.cancelOrder(command: CancelOrderCommand): CommandResult =
    send(command, CommandResult::class.java).await()
