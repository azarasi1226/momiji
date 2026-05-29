package jp.momiji.feature.user.update

import jp.momiji.domain.BusinessError
import jp.momiji.domain.user.Address1
import jp.momiji.domain.user.Address2
import jp.momiji.domain.user.Name
import jp.momiji.domain.user.PhoneNumber
import jp.momiji.domain.user.PostalCode
import jp.momiji.feature.CommandResult
import kotlinx.coroutines.future.await
import org.axonframework.messaging.commandhandling.gateway.CommandGateway
import org.axonframework.modelling.annotation.TargetEntityId

/**
 * CommandHandler に渡される時点で「全フィールド検証済み」であることを型で保証する。
 * 検証は gRPC 入口で [jp.momiji.domain.zipAll] で集約検証して組み立てる。
 */
data class UpdateUserCommand(
    @TargetEntityId
    val id: String,
    val name: Name,
    val phoneNumber: PhoneNumber,
    val postalCode: PostalCode,
    val address1: Address1,
    val address2: Address2,
)

object UpdateUserCommandResult {
    fun success() = CommandResult.success()

    fun userNotFound() = CommandResult.fail(BusinessError("ユーザーが存在しませんでした"))
}

suspend fun CommandGateway.updateUser(command: UpdateUserCommand): CommandResult = send(command, CommandResult::class.java).await()
