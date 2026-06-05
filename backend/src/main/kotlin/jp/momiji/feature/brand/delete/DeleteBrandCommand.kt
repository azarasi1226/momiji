package jp.momiji.feature.brand.delete

import jp.momiji.domain.BusinessError
import jp.momiji.feature.CommandResult
import kotlinx.coroutines.future.await
import org.axonframework.messaging.commandhandling.gateway.CommandGateway
import org.axonframework.modelling.annotation.TargetEntityId

data class DeleteBrandCommand(
    @TargetEntityId
    val id: String,
)

object DeleteBrandCommandResult {
    fun success() = CommandResult.success()

    fun brandNotFound() = CommandResult.fail(BusinessError("ブランドが存在しませんでした"))
}

suspend fun CommandGateway.deleteBrand(command: DeleteBrandCommand): CommandResult = send(command, CommandResult::class.java).await()
