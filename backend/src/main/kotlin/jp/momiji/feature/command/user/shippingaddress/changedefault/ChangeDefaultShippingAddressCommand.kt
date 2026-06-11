package jp.momiji.feature.command.user.shippingaddress.changedefault

import jp.momiji.domain.BusinessError
import jp.momiji.feature.command.CommandResult
import kotlinx.coroutines.future.await
import org.axonframework.messaging.commandhandling.gateway.CommandGateway
import org.axonframework.modelling.annotation.TargetEntityId

/**
 * デフォルト配送先を変更するコマンド。 整合境界は user（State が user 単位）。
 */
data class ChangeDefaultShippingAddressCommand(
    @TargetEntityId
    val userId: String,
    val shippingAddressId: String,
)

object ChangeDefaultShippingAddressCommandResult {
    fun success() = CommandResult.success()

    fun userNotFound() = CommandResult.fail(BusinessError("ユーザーが存在しません"))

    fun addressNotFound() = CommandResult.fail(BusinessError("配送先が存在しません"))
}

suspend fun CommandGateway.changeDefaultShippingAddress(command: ChangeDefaultShippingAddressCommand): CommandResult =
    send(command, CommandResult::class.java).await()
