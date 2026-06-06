package jp.momiji.feature.command.user.delete

import jp.momiji.domain.BusinessError
import jp.momiji.feature.command.CommandResult
import kotlinx.coroutines.future.await
import org.axonframework.messaging.commandhandling.gateway.CommandGateway
import org.axonframework.modelling.annotation.TargetEntityId

data class DeleteUserCommand(
    @TargetEntityId
    val id: String,
)

object DeleteUserCommandResult {
    fun success() = CommandResult.Companion.success()

    fun userNotFound() = CommandResult.Companion.fail(BusinessError("ユーザーが存在しませんでした"))
}

suspend fun CommandGateway.deleteUser(command: DeleteUserCommand): CommandResult = send(command, CommandResult::class.java).await()
