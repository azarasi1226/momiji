package jp.momiji.feature.command.payment.changedefaultcard

import jp.momiji.domain.BusinessError
import jp.momiji.feature.command.CommandResult
import kotlinx.coroutines.future.await
import org.axonframework.messaging.commandhandling.gateway.CommandGateway
import org.axonframework.modelling.annotation.TargetEntityId

/**
 * デフォルトカードを変更するコマンド。 整合境界は user（State が user 単位）。
 */
data class ChangeDefaultCardCommand(
    @TargetEntityId
    val userId: String,
    val paymentMethodId: String,
)

object ChangeDefaultCardCommandResult {
    fun success() = CommandResult.success()

    fun userNotFound() = CommandResult.fail(BusinessError("ユーザーが存在しません"))

    fun cardNotFound() = CommandResult.fail(BusinessError("カードが存在しません"))
}

suspend fun CommandGateway.changeDefaultCard(command: ChangeDefaultCardCommand): CommandResult =
    send(command, CommandResult::class.java).await()
