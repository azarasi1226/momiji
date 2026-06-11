package jp.momiji.feature.command.user.shippingaddress.delete

import jp.momiji.domain.BusinessError
import jp.momiji.feature.command.CommandResult
import kotlinx.coroutines.future.await
import org.axonframework.messaging.commandhandling.gateway.CommandGateway
import org.axonframework.modelling.annotation.TargetEntityId

/**
 * 配送先を削除するコマンド。 整合境界は user（State が user 単位）。
 *
 * 削除したのが default で他の配送先が残る場合は、 最古の配送先を default に昇格させる
 * （不変条件「配送先があれば必ず default が 1 件」の削除側）。
 */
data class DeleteShippingAddressCommand(
    @TargetEntityId
    val userId: String,
    val shippingAddressId: String,
)

object DeleteShippingAddressCommandResult {
    fun success() = CommandResult.success()

    fun userNotFound() = CommandResult.fail(BusinessError("ユーザーが存在しません"))
}

suspend fun CommandGateway.deleteShippingAddress(command: DeleteShippingAddressCommand): CommandResult =
    send(command, CommandResult::class.java).await()
