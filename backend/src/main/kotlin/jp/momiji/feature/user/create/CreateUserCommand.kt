package jp.momiji.feature.user.create

import jp.momiji.domain.BusinessError
import jp.momiji.domain.idp.IdentityProvider
import jp.momiji.domain.user.Email
import jp.momiji.feature.CommandResult
import kotlinx.coroutines.future.await
import org.axonframework.messaging.commandhandling.gateway.CommandGateway

data class CreateUserCommand(
    val oidcIssuer: String,
    val oidcSubject: String,
    val oidcIdentityProvider: IdentityProvider,
    val email: Email,
    val emailVerified: Boolean,
)

object CreateUserCommandResult {
    fun success() = CommandResult.success()

    fun emailNotVerified() = CommandResult.fail(BusinessError("Emailが検証されていません"))
}

suspend fun CommandGateway.createUser(command: CreateUserCommand): CommandResult = send(command, CommandResult::class.java).await()
