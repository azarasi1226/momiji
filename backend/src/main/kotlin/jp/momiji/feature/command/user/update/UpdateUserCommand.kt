package jp.momiji.feature.command.user.update

import jp.momiji.domain.BusinessError
import jp.momiji.domain.user.Name
import jp.momiji.feature.command.CommandResult
import kotlinx.coroutines.future.await
import org.axonframework.messaging.commandhandling.gateway.CommandGateway
import org.axonframework.modelling.annotation.TargetEntityId

/**
 * CommandHandler に渡される時点で「全フィールド検証済み」であることを型で保証する。
 * プロフィールは name のみ（住所・電話は配送先ユースケースが持つ）。
 */
data class UpdateUserCommand(
    @TargetEntityId
    val id: String,
    val name: Name,
)

object UpdateUserCommandResult {
    fun success() = CommandResult.Companion.success()

    fun userNotFound() = CommandResult.Companion.fail(BusinessError("ユーザーが存在しませんでした"))
}

suspend fun CommandGateway.updateUser(command: UpdateUserCommand): CommandResult = send(command, CommandResult::class.java).await()
