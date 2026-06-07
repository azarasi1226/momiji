package jp.momiji.feature.command.brand.update

import jp.momiji.domain.BusinessError
import jp.momiji.domain.brand.BrandDescription
import jp.momiji.domain.brand.BrandName
import jp.momiji.feature.command.CommandResult
import kotlinx.coroutines.future.await
import org.axonframework.messaging.commandhandling.gateway.CommandGateway
import org.axonframework.modelling.annotation.TargetEntityId

data class UpdateBrandCommand(
    @TargetEntityId
    val id: String,
    val name: BrandName,
    val description: BrandDescription,
)

object UpdateBrandCommandResult {
    fun success() = CommandResult.Companion.success()

    fun brandNotFound() = CommandResult.Companion.fail(BusinessError("ブランドが存在しませんでした"))
}

suspend fun CommandGateway.updateBrand(command: UpdateBrandCommand): CommandResult = send(command, CommandResult::class.java).await()
