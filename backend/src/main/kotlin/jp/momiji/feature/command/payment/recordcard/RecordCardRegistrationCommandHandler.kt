package jp.momiji.feature.command.payment.recordcard

import jp.momiji.event.payment.CardRegisteredEvent
import jp.momiji.event.payment.DefaultCardChangedEvent
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

        // 「カードが増えた」と「default になった」はイベントを分ける（default の変化経路を
        // DefaultCardChangedEvent の 1 種類に一本化するため。配送先と同じ設計）。
        eventAppender.append(
            CardRegisteredEvent(
                userId = command.userId,
                paymentMethodId = command.paymentMethodId,
                brand = command.brand,
                last4 = command.last4,
                expMonth = command.expMonth,
                expYear = command.expYear,
            ),
        )

        // default になる条件: default が 1 件も存在しない（初回カード。 万一 default 不在の
        // 不変条件が壊れていてもここで自己修復される）。
        if (state.cards.values.none { it.isDefault }) {
            eventAppender.append(
                DefaultCardChangedEvent(
                    userId = command.userId,
                    paymentMethodId = command.paymentMethodId,
                ),
            )
        }
        return RecordCardRegistrationCommandResult.success()
    }
}
