package jp.momiji.feature.command.brand.create

import jp.momiji.domain.brand.BrandDescription
import jp.momiji.domain.brand.BrandName
import jp.momiji.feature.command.CommandResult
import kotlinx.coroutines.future.await
import org.axonframework.messaging.commandhandling.gateway.CommandGateway
import org.axonframework.modelling.annotation.TargetEntityId

data class CreateBrandCommand(
    @TargetEntityId
    val id: String,
    val name: BrandName,
    val description: BrandDescription,
)

object CreateBrandCommandResult {
    fun success() = CommandResult.Companion.success()
}

suspend fun CommandGateway.createBrand(command: CreateBrandCommand): CommandResult = send(command, CommandResult::class.java).await()
