package jp.momiji.feature.user.changeemail.confirm

import jp.momiji.feature.CommandResult
import jp.momiji.feature.Error
import org.axonframework.messaging.commandhandling.gateway.CommandGateway

data class ConfirmEmailChangeCommand(
  val userId: String,
  val token: String,
)

object ConfirmEmailChangeCommandResult {
  fun success() = CommandResult.success()
  fun invalidToken() = CommandResult.faile(Error("無効または期限切れのトークンです"))
  fun emailAlreadyInUse() = CommandResult.faile(Error("このメールアドレスは既に使用されています"))
  fun userMismatch() = CommandResult.faile(Error("このメールアドレス変更リクエストは別のユーザーのものです"))
}

fun CommandGateway.confirmEmailChange(command: ConfirmEmailChangeCommand): CommandResult =
  this.sendAndWait(command, CommandResult::class.java)
