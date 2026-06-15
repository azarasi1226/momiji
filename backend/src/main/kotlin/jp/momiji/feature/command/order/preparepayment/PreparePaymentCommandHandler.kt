package jp.momiji.feature.command.order.preparepayment

import jp.momiji.event.order.OrderPaymentPreparedEvent
import jp.momiji.feature.command.CommandResult
import jp.momiji.feature.command.order.start.OrderState
import org.axonframework.messaging.commandhandling.annotation.CommandHandler
import org.axonframework.messaging.eventhandling.gateway.EventAppender
import org.axonframework.modelling.annotation.InjectEntity
import org.springframework.stereotype.Component

/**
 * 決済準備の記録 CommandHandler。 **STARTED のときだけ** PAYMENT_PENDING へ遷移させる（pi_/pm_ を記録）。
 *
 * 既に PAYMENT_PENDING/PAID/FAILED なら no-op 成功 ── 決済準備の**冪等な再送**（同一注文の二重記録を防ぐ）。
 * 決済失敗時は注文ごと FAILED にする方針（再準備しない）なので、 PAYMENT_PENDING からの再記録は不要。
 * 注文が存在しない（status==null）のは異常（service が read model で存在を確認済み）なので orderNotFound。
 */
@Component
class PreparePaymentCommandHandler {
    @CommandHandler
    fun handle(
        command: PreparePaymentCommand,
        @InjectEntity(idProperty = "orderId") order: OrderState,
        eventAppender: EventAppender,
    ): CommandResult {
        if (order.notStarted) {
            return PreparePaymentCommandResult.orderNotFound()
        }

        // 冪等性: STARTED のときだけ記録。 PAYMENT_PENDING への再送・既決済は no-op。
        if (order.isStarted) {
            eventAppender.append(
                OrderPaymentPreparedEvent(
                    orderId = command.orderId,
                    paymentMethodId = command.paymentMethodId,
                    paymentIntentId = command.paymentIntentId,
                ),
            )
        }
        return PreparePaymentCommandResult.success()
    }
}
