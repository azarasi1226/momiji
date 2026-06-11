package jp.momiji.feature.command.payment.changedefaultcard

import jp.momiji.event.payment.DefaultCardChangedEvent
import jp.momiji.feature.command.CommandResult
import jp.momiji.feature.command.payment.UserPaymentState
import org.axonframework.messaging.commandhandling.annotation.CommandHandler
import org.axonframework.messaging.eventhandling.gateway.EventAppender
import org.axonframework.modelling.annotation.InjectEntity
import org.springframework.stereotype.Component

@Component
class ChangeDefaultCardCommandHandler {
    @CommandHandler
    fun handle(
        command: ChangeDefaultCardCommand,
        @InjectEntity state: UserPaymentState,
        eventAppender: EventAppender,
    ): CommandResult {
        if (!state.userExists) {
            return ChangeDefaultCardCommandResult.userNotFound()
        }
        // 削除と違い「無いカードをデフォルトにする」はゴール不能なので、 不在は冪等成功でなく error。
        val card =
            state.cards[command.paymentMethodId]
                ?: return ChangeDefaultCardCommandResult.cardNotFound()

        // 冪等性: 既にデフォルトなら状態は変わらないのでイベントを出さず成功。
        if (card.isDefault) {
            return ChangeDefaultCardCommandResult.success()
        }

        eventAppender.append(
            DefaultCardChangedEvent(
                userId = command.userId,
                paymentMethodId = command.paymentMethodId,
            ),
        )
        return ChangeDefaultCardCommandResult.success()
    }
}
