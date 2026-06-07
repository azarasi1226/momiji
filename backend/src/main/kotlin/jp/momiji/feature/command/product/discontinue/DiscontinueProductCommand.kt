package jp.momiji.feature.command.product.discontinue

import jp.momiji.domain.BusinessError
import jp.momiji.feature.command.CommandResult
import kotlinx.coroutines.future.await
import org.axonframework.messaging.commandhandling.gateway.CommandGateway
import org.axonframework.modelling.annotation.TargetEntityId

data class DiscontinueProductCommand(
    @TargetEntityId
    val id: String,
)

object DiscontinueProductCommandResult {
    fun success() = CommandResult.Companion.success()

    fun productNotFound() = CommandResult.Companion.fail(BusinessError("商品が存在しませんでした"))
}

suspend fun CommandGateway.discontinueProduct(command: DiscontinueProductCommand): CommandResult =
    send(command, CommandResult::class.java).await()
