package jp.momiji.feature.command.order.complete

import jp.momiji.event.order.OrderCompletedEvent
import jp.momiji.event.stock.StockReservationCommittedEvent
import jp.momiji.feature.command.CommandResult
import jp.momiji.feature.command.order.OrderState
import jp.momiji.feature.command.order.ProductsState
import org.axonframework.messaging.commandhandling.annotation.CommandHandler
import org.axonframework.messaging.eventhandling.gateway.EventAppender
import org.axonframework.modelling.annotation.InjectEntity
import org.springframework.stereotype.Component

/**
 * 注文完了の記録 CommandHandler。 **SHIPPED のときだけ** COMPLETED へ遷移させ、 予約していた在庫を引き当て確定する。
 *
 * ガードを整合境界（ここ）に集約する（ADR 0013）:
 * - **COMPLETED** → no-op 成功（完了依頼の重複・再送を冪等に握る。 在庫の二重確定も防ぐ）。
 * - **SHIPPED** → [OrderCompletedEvent] ＋ 予約分の [StockReservationCommittedEvent]（onHand・reserved を減らす）を **1 append** で発行。
 * - **それ以外（未発送・不在）** → [CompleteOrderCommandResult.cannotComplete]。 reactor 経由では到達しないが防御。
 */
@Component
class CompleteOrderCommandHandler {
    @CommandHandler
    fun handle(
        command: CompleteOrderCommand,
        @InjectEntity(idProperty = "orderId") order: OrderState,
        @InjectEntity(idProperty = "orderProductIds") products: ProductsState,
        eventAppender: EventAppender,
    ): CommandResult {
        // 冪等性: 既に完了なら no-op 成功（在庫を二重確定しない）。
        if (order.isCompleted) {
            return CompleteOrderCommandResult.success()
        }
        // SHIPPED のときだけ完了を記録し、 予約在庫を引き当て確定する。
        if (order.isShipped) {
            // 出荷確定: 各商品の onHand・reserved を予約分だけ減らす（確定後の絶対値を載せる）。
            val committedEvents =
                order.reservedItems.map { item ->
                    StockReservationCommittedEvent(
                        productId = item.productId,
                        orderId = command.orderId,
                        quantity = item.quantity,
                        onHandQuantity = products.onHandOf(item.productId) - item.quantity,
                        reservedQuantity = products.reservedOf(item.productId) - item.quantity,
                    )
                }
            eventAppender.append(
                OrderCompletedEvent(orderId = command.orderId),
                *committedEvents.toTypedArray(),
            )
            return CompleteOrderCommandResult.success()
        }
        // 未発送・不在 → 完了不能。
        return CompleteOrderCommandResult.cannotComplete()
    }
}
