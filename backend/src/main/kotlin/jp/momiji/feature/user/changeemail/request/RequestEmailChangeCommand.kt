package jp.momiji.feature.user.changeemail.request

import jp.momiji.feature.CommandResult
import jp.momiji.feature.Error
import org.axonframework.messaging.commandhandling.gateway.CommandGateway

data class RequestEmailChangeCommand(
  val userId: String,
  val newEmail: String,
)

object RequestEmailChangeCommandResult {
  fun success() = CommandResult.success()
  fun emailAlreadyInUse() = CommandResult.faile(Error("このメールアドレスは既に使用されています"))
}

fun CommandGateway.requestEmailChange(command: RequestEmailChangeCommand): CommandResult =
  this.sendAndWait(command, CommandResult::class.java)
