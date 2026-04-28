package jp.momiji.feature.user.create

import jp.momiji.feature.CommandResult
import jp.momiji.feature.Error
import kotlinx.coroutines.future.await
import org.axonframework.messaging.commandhandling.gateway.CommandGateway

data class CreateUserCommand(
  val oidcIssuer: String,
  val oidcSubject: String,
  val oidcIdentityProvider: String,
  val email: String,
  val emailVerified: Boolean
)

object CreateUserCommandResult {
  fun success() = CommandResult.success()
  fun emailNotVerified() = CommandResult.fail(Error("Emailが検証されていません"))
}

suspend fun CommandGateway.createUser(command: CreateUserCommand): CommandResult {
  return send(command, CommandResult::class.java).await()
}