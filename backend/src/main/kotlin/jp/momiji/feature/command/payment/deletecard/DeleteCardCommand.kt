package jp.momiji.feature.command.payment.deletecard

import jp.momiji.domain.BusinessError
import jp.momiji.feature.command.CommandResult
import kotlinx.coroutines.future.await
import org.axonframework.messaging.commandhandling.gateway.CommandGateway
import org.axonframework.modelling.annotation.TargetEntityId

/**
 * 登録済みカード（pm_）を削除するコマンド。 整合境界は user（State が user 単位）。
 *
 * momiji 側の削除を [jp.momiji.event.payment.CardDeletedEvent] として記録する。 Stripe の DetachPaymentMethod は
 * 後続の冪等な外部副作用ハンドラ [CardDetacher] が行う。
 */
data class DeleteCardCommand(
    @TargetEntityId
    val userId: String,
    val paymentMethodId: String,
)

object DeleteCardCommandResult {
    fun success() = CommandResult.success()

    fun userNotFound() = CommandResult.fail(BusinessError("ユーザーが存在しません"))
}

suspend fun CommandGateway.deleteCard(command: DeleteCardCommand): CommandResult = send(command, CommandResult::class.java).await()
