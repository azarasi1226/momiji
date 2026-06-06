package jp.momiji.feature.command.user.changeemail.confirm

import jp.momiji.domain.BusinessError
import jp.momiji.domain.user.EmailChangeToken
import jp.momiji.feature.command.CommandResult
import kotlinx.coroutines.future.await
import org.axonframework.messaging.commandhandling.gateway.CommandGateway
import org.axonframework.modelling.annotation.TargetEntityId

data class ConfirmEmailChangeCommand(
    @TargetEntityId
    val userId: String,
    val token: EmailChangeToken,
)

object ConfirmEmailChangeCommandResult {
    fun success() = CommandResult.Companion.success()

    fun userNotFound() = CommandResult.Companion.fail(BusinessError("ユーザーが存在しませんでした"))

    fun invalidToken() = CommandResult.Companion.fail(BusinessError("無効または期限切れのトークンです"))

    fun emailAlreadyInUse() = CommandResult.Companion.fail(BusinessError("このメールアドレスは既に使用されています"))

    fun userMismatch() = CommandResult.Companion.fail(BusinessError("このメールアドレス変更リクエストは別のユーザーのものです"))
}

suspend fun CommandGateway.confirmEmailChange(command: ConfirmEmailChangeCommand): CommandResult =
    send(command, CommandResult::class.java).await()
