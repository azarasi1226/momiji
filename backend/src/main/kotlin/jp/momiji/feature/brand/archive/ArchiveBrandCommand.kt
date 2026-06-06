package jp.momiji.feature.brand.archive

import jp.momiji.domain.BusinessError
import jp.momiji.feature.CommandResult
import kotlinx.coroutines.future.await
import org.axonframework.messaging.commandhandling.gateway.CommandGateway
import org.axonframework.modelling.annotation.TargetEntityId

data class ArchiveBrandCommand(
    @TargetEntityId
    val id: String,
)

object ArchiveBrandCommandResult {
    fun success() = CommandResult.success()

    fun brandNotFound() = CommandResult.fail(BusinessError("ブランドが存在しませんでした"))
}

suspend fun CommandGateway.archiveBrand(command: ArchiveBrandCommand): CommandResult = send(command, CommandResult::class.java).await()
