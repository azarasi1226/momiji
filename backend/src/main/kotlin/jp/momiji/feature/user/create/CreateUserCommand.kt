package jp.momiji.feature.user.create

import jp.momiji.feature.CommandResult
import jp.momiji.feature.Error
import org.axonframework.messaging.commandhandling.gateway.CommandGateway

data class CreateUserCommand(
  val oidcIssuer: String,
  val oidcSubject: String,
  val email: String,
  val emailVerified: Boolean
)

object CreateUserCommandResult {
  fun success() = CommandResult.success()
  fun emailNotVerified() = CommandResult.fail(Error("Emailが検証されていません"))
}

fun CommandGateway.createUser(command: CreateUserCommand): CommandResult =
  this.sendAndWait(command, CommandResult::class.java)