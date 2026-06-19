package jp.momiji.feature.command.order.cancel

import jp.momiji.event.order.OrderCancelledEvent
import jp.momiji.event.stock.StockReservationReleasedEvent
import jp.momiji.feature.command.CommandResult
import jp.momiji.feature.command.order.OrderState
import jp.momiji.feature.command.order.ProductsState
import org.axonframework.messaging.commandhandling.annotation.CommandHandler
import org.axonframework.messaging.eventhandling.gateway.EventAppender
import org.axonframework.modelling.annotation.InjectEntity
import org.springframework.stereotype.Component

/**
 * 注文キャンセルの CommandHandler。 **発送前（STARTED/PAYMENT_PENDING/PAID）のときだけ** 予約を解放して CANCELLED にする。
 *
 * - 発送済み以降（SHIPPED/COMPLETED）→ [CancelOrderCommandResult.alreadyShipped]（キャンセル不可）。
 * - 既に CANCELLED/FAILED、 または不在 → no-op 成功（冪等な再送・失効済みの取消要求を握る）。
 * - PAID（課金確定済み）→ 返金対象の pi_ を [OrderCancelledEvent] に載せる（実際の返金は [OrderRefunder] が非同期に行う）。
 *   未課金（STARTED/PAYMENT_PENDING）→ 返金不要（pi_ は載せない）。
 */
@Component
class CancelOrderCommandHandler {
    @CommandHandler
    fun handle(
        command: CancelOrderCommand,
        @InjectEntity(idProperty = "orderId") order: OrderState,
        @InjectEntity(idProperty = "orderProductIds") products: ProductsState,
        eventAppender: EventAppender,
    ): CommandResult {
        // 発送済み以降は取消不可（在庫は確定済み or 確定途上）。
        if (order.isShippedOrBeyond) {
            return CancelOrderCommandResult.alreadyShipped()
        }
        // 発送前でなければ（既に CANCELLED/FAILED・不在）no-op 成功。 冪等。
        if (!order.canCancel) {
            return CancelOrderCommandResult.success()
        }

        // 整合境界の検証: read model 由来の productIds が予約全商品をカバーしてること（負の予約数を焼かない防御）。
        order.requireReservedProductsCovered(command.orderId, command.productIds)

        // 予約解放（PAID も発送までは予約を保持しているので解放対象）。
        val releasedEvents =
            order.reservedItems.map { item ->
                StockReservationReleasedEvent(
                    productId = item.productId,
                    orderId = command.orderId,
                    quantity = item.quantity,
                    reservedQuantity = products.reservedOf(item.productId) - item.quantity,
                )
            }

        // 返金対象は PAID（課金確定済み）のときだけ。 未課金は null（返金不要）。
        val refundPaymentIntentId = if (order.isPaid) order.paymentIntentId else null

        val cancelledEvent =
            OrderCancelledEvent(
                orderId = command.orderId,
                reason = command.reason.name,
                refundPaymentIntentId = refundPaymentIntentId,
            )
        eventAppender.append(cancelledEvent, *releasedEvents.toTypedArray())

        return CancelOrderCommandResult.success()
    }
}
