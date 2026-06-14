package jp.momiji.feature.command.order.expire

import jp.momiji.domain.order.OrderFailureReason
import jp.momiji.event.order.OrderFailedEvent
import jp.momiji.event.stock.StockReservationReleasedEvent
import jp.momiji.feature.command.CommandResult
import jp.momiji.feature.command.order.start.OrderState
import jp.momiji.feature.command.order.start.ProductsState
import org.axonframework.messaging.commandhandling.annotation.CommandHandler
import org.axonframework.messaging.eventhandling.gateway.EventAppender
import org.axonframework.modelling.annotation.InjectEntity
import org.springframework.stereotype.Component

/**
 * 注文を失効させる CommandHandler（予約タイムアウトの補償）。
 *
 * **STARTED のときだけ**失効させる。 既に PAID/FAILED、 または存在しない（status==null）なら no-op で成功（冪等）。
 * これで「期限切れ直前に支払い成功」レースでも、 先に PAID になっていれば解放しない（二重に撃たれても安全）。
 */
@Component
class ExpireOrderCommandHandler {
    @CommandHandler
    fun handle(
        command: ExpireOrderCommand,
        @InjectEntity(idProperty = "orderId") order: OrderState,
        @InjectEntity(idProperty = "orderProductIds") products: ProductsState,
        eventAppender: EventAppender,
    ): CommandResult {
        if (!order.isStarted) {
            return ExpireOrderCommandResult.success()
        }

        // 解放する個数は OrderState の予約スナップショット（権威）から。 reservedQuantity の絶対値は ProductsState の現在値から。
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
            OrderFailedEvent(orderId = command.orderId, reason = OrderFailureReason.EXPIRED.name)
        eventAppender.append(orderFailedEvent, *releasedEvents.toTypedArray())

        return ExpireOrderCommandResult.success()
    }
}
