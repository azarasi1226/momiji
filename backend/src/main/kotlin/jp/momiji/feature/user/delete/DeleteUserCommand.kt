package jp.momiji.feature.user.delete

import jp.momiji.feature.CommandResult
import jp.momiji.feature.Error
import org.axonframework.messaging.commandhandling.gateway.CommandGateway
import org.axonframework.modelling.annotation.TargetEntityId

data class DeleteUserCommand(
  @TargetEntityId
  val id: String,
)

object DeleteUserCommandResult {
  fun success() = CommandResult.success()
  fun userNotFound() = CommandResult.fail(Error("ユーザーが存在しませんでした"))
}

fun CommandGateway.deleteUser(command: DeleteUserCommand): CommandResult =
  this.sendAndWait(command, CommandResult::class.java)
