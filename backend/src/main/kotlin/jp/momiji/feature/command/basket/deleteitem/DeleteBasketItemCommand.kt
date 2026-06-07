package jp.momiji.feature.command.basket.deleteitem

import jp.momiji.domain.BusinessError
import jp.momiji.feature.command.CommandResult
import kotlinx.coroutines.future.await
import org.axonframework.messaging.commandhandling.gateway.CommandGateway
import org.axonframework.modelling.annotation.TargetEntityId

data class DeleteBasketItemCommand(
    @TargetEntityId
    val userId: String,
    val productId: String,
)

object DeleteBasketItemCommandResult {
    fun success() = CommandResult.success()

    fun userNotFound() = CommandResult.fail(BusinessError("ユーザーが存在しません"))
}

suspend fun CommandGateway.deleteBasketItem(command: DeleteBasketItemCommand): CommandResult =
    send(command, CommandResult::class.java).await()
