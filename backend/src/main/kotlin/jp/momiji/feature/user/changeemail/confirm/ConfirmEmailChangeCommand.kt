package jp.momiji.feature.user.changeemail.confirm

import jp.momiji.feature.CommandResult
import jp.momiji.feature.Error
import kotlinx.coroutines.future.await
import org.axonframework.messaging.commandhandling.gateway.CommandGateway
import org.axonframework.modelling.annotation.TargetEntityId

data class ConfirmEmailChangeCommand(
  @TargetEntityId
  val userId: String,
  val token: String,
)

object ConfirmEmailChangeCommandResult {
  fun success() = CommandResult.success()
  fun userNotFound() = CommandResult.fail(Error("ユーザーが存在しませんでした"))
  fun invalidToken() = CommandResult.fail(Error("無効または期限切れのトークンです"))
  fun emailAlreadyInUse() = CommandResult.fail(Error("このメールアドレスは既に使用されています"))
  fun userMismatch() = CommandResult.fail(Error("このメールアドレス変更リクエストは別のユーザーのものです"))
}

suspend fun CommandGateway.confirmEmailChange(command: ConfirmEmailChangeCommand): CommandResult =
  send(command, CommandResult::class.java).await()
