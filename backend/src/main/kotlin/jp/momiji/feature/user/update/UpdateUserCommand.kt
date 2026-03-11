package jp.momiji.feature.user.update

import jp.momiji.feature.CommandResult
import jp.momiji.feature.Error
import org.axonframework.messaging.commandhandling.gateway.CommandGateway
import org.axonframework.modelling.annotation.TargetEntityId

data class UpdateUserCommand(
  @TargetEntityId
  val id: String,
  val name: String,
  val phoneNumber: String,
  val postalCode: String,
  val address1: String,
  val address2: String
)

object UpdateUserCommandResult {
  fun success() = CommandResult.success()
  fun userNotFound() = CommandResult.fail(Error("ユーザーが存在しませんでした"))
}

fun CommandGateway.updateUser(command: UpdateUserCommand): CommandResult =
  this.sendAndWait(command, CommandResult::class.java)
