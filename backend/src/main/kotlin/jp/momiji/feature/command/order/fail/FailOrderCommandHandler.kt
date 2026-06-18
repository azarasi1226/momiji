package jp.momiji.feature.command.order.fail

import jp.momiji.event.order.OrderFailedEvent
import jp.momiji.event.stock.StockReservationReleasedEvent
import jp.momiji.feature.command.CommandResult
import jp.momiji.feature.command.order.OrderState
import jp.momiji.feature.command.order.ProductsState
import org.axonframework.messaging.commandhandling.annotation.CommandHandler
import org.axonframework.messaging.eventhandling.gateway.EventAppender
import org.axonframework.modelling.annotation.InjectEntity
import org.springframework.stereotype.Component

/**
 * 注文を失敗させる
 *
 * **releasable（STARTED or PAYMENT_PENDING）のときだけ**解放する。
 * 既に PAID/FAILED、 または存在しないなら no-op で成功にする（冪等）。
 */
@Component
class FailOrderCommandHandler {
    @CommandHandler
    fun handle(
        command: FailOrderCommand,
        @InjectEntity(idProperty = "orderId") order: OrderState,
        @InjectEntity(idProperty = "orderProductIds") products: ProductsState,
        eventAppender: EventAppender,
    ): CommandResult {
        // 冪等性：注文が失敗状態にできない場合は成功とする。
        if (!order.canReleaseReservation) {
            return FailOrderCommandResult.success()
        }

        // 保障トランザクションとしてアイテムの予約を解放していく
        val releasedEvents =
            order.reservedItems.map { item ->
                StockReservationReleasedEvent(
                    productId = item.productId,
                    orderId = command.orderId,
                    quantity = item.quantity,
                    reservedQuantity = products.reservedOf(item.productId) - item.quantity,
                )
            }
        val orderFailedEvent =
            OrderFailedEvent(orderId = command.orderId, reason = command.reason.name)
        eventAppender.append(orderFailedEvent, *releasedEvents.toTypedArray())

        return FailOrderCommandResult.success()
    }
}
