package jp.momiji.feature.command.payment.recordcard

import jp.momiji.event.payment.CardRegisteredEvent
import jp.momiji.feature.command.CommandResult
import jp.momiji.feature.command.payment.UserPaymentState
import org.axonframework.messaging.commandhandling.annotation.CommandHandler
import org.axonframework.messaging.eventhandling.gateway.EventAppender
import org.axonframework.modelling.annotation.InjectEntity
import org.springframework.stereotype.Component

@Component
class RecordCardRegistrationCommandHandler {
    @CommandHandler
    fun handle(
        command: RecordCardRegistrationCommand,
        @InjectEntity state: UserPaymentState,
        eventAppender: EventAppender,
    ): CommandResult {
        if (!state.userExists) {
            return RecordCardRegistrationCommandResult.userNotFound()
        }
        // 冪等性: webhook は再送されうるため。
        // 同じ pm_ が既に登録済みなら新規イベントを出さず成功。
        if (state.cards.containsKey(command.paymentMethodId)) {
            return RecordCardRegistrationCommandResult.success()
        }

        // default が存在しなければこのカードを default にする。
        // 不変条件「カードがあれば必ず default が 1 枚」の登録側。
        val isDefault = state.cards.values.none { it.isDefault }

        eventAppender.append(
            CardRegisteredEvent(
                userId = command.userId,
                paymentMethodId = command.paymentMethodId,
                brand = command.brand,
                last4 = command.last4,
                expMonth = command.expMonth,
                expYear = command.expYear,
                default = isDefault,
            ),
        )
        return RecordCardRegistrationCommandResult.success()
    }
}
