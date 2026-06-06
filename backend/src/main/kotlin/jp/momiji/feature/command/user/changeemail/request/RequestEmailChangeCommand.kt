package jp.momiji.feature.command.user.changeemail.request

import jp.momiji.domain.BusinessError
import jp.momiji.domain.user.Email
import jp.momiji.feature.command.CommandResult
import kotlinx.coroutines.future.await
import org.axonframework.messaging.commandhandling.gateway.CommandGateway
import org.axonframework.modelling.annotation.TargetEntityId

data class RequestEmailChangeCommand(
    @TargetEntityId
    val userId: String,
    val newEmail: Email,
)

object RequestEmailChangeCommandResult {
    fun success() = CommandResult.Companion.success()

    fun userNotFound() = CommandResult.Companion.fail(BusinessError("ユーザーが存在しませんでした"))

    fun emailAlreadyInUse() = CommandResult.Companion.fail(BusinessError("このメールアドレスは既に使用されています"))
}

suspend fun CommandGateway.requestEmailChange(command: RequestEmailChangeCommand): CommandResult =
    send(command, CommandResult::class.java).await()
