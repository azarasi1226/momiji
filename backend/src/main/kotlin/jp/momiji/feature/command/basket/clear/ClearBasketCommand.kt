package jp.momiji.feature.command.basket.clear

import jp.momiji.domain.BusinessError
import jp.momiji.feature.command.CommandResult
import kotlinx.coroutines.future.await
import org.axonframework.messaging.commandhandling.gateway.CommandGateway
import org.axonframework.modelling.annotation.TargetEntityId

data class ClearBasketCommand(
    @TargetEntityId
    val userId: String,
)

object ClearBasketCommandResult {
    fun success() = CommandResult.success()

    fun userNotFound() = CommandResult.fail(BusinessError("ユーザーが存在しません"))
}

suspend fun CommandGateway.clearBasket(command: ClearBasketCommand): CommandResult = send(command, CommandResult::class.java).await()
