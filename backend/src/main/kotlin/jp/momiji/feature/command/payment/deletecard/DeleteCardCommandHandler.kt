package jp.momiji.feature.command.payment.deletecard

import jp.momiji.event.payment.CardDeletedEvent
import jp.momiji.event.payment.DefaultCardChangedEvent
import jp.momiji.feature.command.CommandResult
import jp.momiji.feature.command.payment.UserPaymentState
import org.axonframework.messaging.commandhandling.annotation.CommandHandler
import org.axonframework.messaging.eventhandling.gateway.EventAppender
import org.axonframework.modelling.annotation.InjectEntity
import org.springframework.stereotype.Component

@Component
class DeleteCardCommandHandler {
    @CommandHandler
    fun handle(
        command: DeleteCardCommand,
        @InjectEntity state: UserPaymentState,
        eventAppender: EventAppender,
    ): CommandResult {
        if (!state.userExists) {
            return DeleteCardCommandResult.userNotFound()
        }
        // 冪等性: 既に削除済み（カードが無い）なら成功とする
        // エラーにせず no-op で成功を返す（二重クリックや再送でユーザーにエラーを見せない）。
        val card =
            state.cards[command.paymentMethodId]
                ?: return DeleteCardCommandResult.success()

        eventAppender.append(
            CardDeletedEvent(
                userId = command.userId,
                paymentMethodId = command.paymentMethodId,
            ),
        )

        // 不変条件「カードが 1 枚でもあれば必ず default が 1 枚ある」を守る:
        // default を削除して他のカードが残るなら、 最古の残カード（cards は再生順 = 登録順の LinkedHashMap）を昇格させる。
        if (card.isDefault) {
            val successor = state.cards.keys.firstOrNull { it != command.paymentMethodId }
            if (successor != null) {
                eventAppender.append(
                    DefaultCardChangedEvent(
                        userId = command.userId,
                        paymentMethodId = successor,
                    ),
                )
            }
        }
        return DeleteCardCommandResult.success()
    }
}
