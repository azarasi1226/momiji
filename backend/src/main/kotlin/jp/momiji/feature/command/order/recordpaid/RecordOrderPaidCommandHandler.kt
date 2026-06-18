package jp.momiji.feature.command.order.recordpaid

import jp.momiji.event.order.OrderPaidEvent
import jp.momiji.feature.command.order.OrderState
import org.axonframework.messaging.commandhandling.annotation.CommandHandler
import org.axonframework.messaging.eventhandling.gateway.EventAppender
import org.axonframework.modelling.annotation.InjectEntity
import org.springframework.stereotype.Component

/**
 * 決済成功の記録 CommandHandler。
 *
 * - **未確定（STARTED / PAYMENT_PENDING）** → [OrderPaidEvent] を発行して PAID に。 返金不要。
 * - **既に PAID** → 冪等 no-op。 返金不要。
 * - **FAILED（期限切れで解放済み）/ 不在** → もう履行できない（在庫を解放済み）→ **返金必要**。
 *   これが「期限切れ後に課金成功」レースのバックストップ。
 */
@Component
class RecordOrderPaidCommandHandler {
    @CommandHandler
    fun handle(
        command: RecordOrderPaidCommand,
        @InjectEntity(idProperty = "orderId") order: OrderState,
        eventAppender: EventAppender,
    ): RecordOrderPaidResult {
        // 冪等性: 既に決済済み or それ以降（PAID/SHIPPED/COMPLETED）なら no-op 成功。
        // webhook 再送や、 PAID から進行した後の重複でも、 返金せず冪等に握る。
        if (order.isPaidOrBeyond) {
            return RecordOrderPaidResult.paid()
        }
        // PAYMENT_PENDING（決済着手済み・確定待ち）のときだけ PAID にする。
        // 決済成功 webhook は決済準備（OrderPaymentPrepared 記録）の後にしか来ないので、 ここは必ず PAYMENT_PENDING。
        if (order.isPaymentPending) {
            eventAppender.append(OrderPaidEvent(orderId = command.orderId))
            return RecordOrderPaidResult.paid()
        }
        // FAILED（解放済み）または不在 → 履行不能。 課金は返金で戻す。 （STARTED は到達しないので無視。）
        return RecordOrderPaidResult.needsRefund()
    }
}
