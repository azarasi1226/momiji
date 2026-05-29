package jp.momiji.feature.user.changeemail.request

import jp.momiji.domain.BusinessError
import jp.momiji.domain.user.Email
import jp.momiji.feature.CommandResult
import kotlinx.coroutines.future.await
import org.axonframework.messaging.commandhandling.gateway.CommandGateway
import org.axonframework.modelling.annotation.TargetEntityId

data class RequestEmailChangeCommand(
    @TargetEntityId
    val userId: String,
    val newEmail: Email,
)

object RequestEmailChangeCommandResult {
    fun success() = CommandResult.success()

    fun userNotFound() = CommandResult.fail(BusinessError("ユーザーが存在しませんでした"))

    fun emailAlreadyInUse() = CommandResult.fail(BusinessError("このメールアドレスは既に使用されています"))
}

suspend fun CommandGateway.requestEmailChange(command: RequestEmailChangeCommand): CommandResult =
    send(command, CommandResult::class.java).await()
