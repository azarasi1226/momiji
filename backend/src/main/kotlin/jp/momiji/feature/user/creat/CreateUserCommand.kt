package jp.momiji.feature.user.creat

import jp.momiji.feature.CommandResult
import jp.momiji.feature.Error

data class CreateUserCommand(
  val oidcIssuer: String,
  val oidcSubject: String,
  val email: String,
  val emailVerified: Boolean
)

object CreateUserCommandResult {
  fun success() = CommandResult.success()
  fun emailError() = CommandResult.faile(Error("Emailが検証されていません"))
}
