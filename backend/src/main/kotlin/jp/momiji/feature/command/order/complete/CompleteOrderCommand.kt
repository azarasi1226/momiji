package jp.momiji.feature.command.order.complete

import jp.momiji.domain.BusinessError
import jp.momiji.feature.command.CommandResult
import jp.momiji.feature.command.order.OrderProductIds
import kotlinx.coroutines.future.await
import org.axonframework.messaging.commandhandling.gateway.CommandGateway

/**
 * 注文完了のコマンド（SHIPPED → COMPLETED）。 完了時に予約していた在庫を**引き当て確定**（出荷＝onHand と reserved を減らす）する。
 *
 * 発送（[jp.momiji.event.order.OrderShippedEvent]）を拾った reactor（[CompleteShippedOrderProcessManager]）が撃つ。
 *
 * 整合境界は **order_id ＋ 全 product_id**:
 * - order_id（[jp.momiji.feature.command.order.OrderState]）: SHIPPED のときだけ完了（ガード・冪等）
 * - product_id（×n, [jp.momiji.feature.command.order.ProductsState]）: 各商品の現在の在庫・予約数（確定後の絶対値を出すため）
 */
data class CompleteOrderCommand(
    val orderId: String,
    val productIds: List<String>,
) {
    // ProductsState の id（@InjectEntity(idProperty = "orderProductIds")）。
    val orderProductIds: OrderProductIds
        get() = OrderProductIds(productIds)
}

object CompleteOrderCommandResult {
    fun success() = CommandResult.success()

    /** SHIPPED でも COMPLETED でもない注文への完了依頼。 通常 reactor 経由では到達しない異常。 */
    fun cannotComplete() = CommandResult.fail(BusinessError("この注文は完了できません"))
}

suspend fun CommandGateway.completeOrder(command: CompleteOrderCommand): CommandResult = send(command, CommandResult::class.java).await()
