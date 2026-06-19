package jp.momiji.feature.command.brand.archive

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jp.momiji.domain.ValidationException
import jp.momiji.feature.command.CommandResult
import jp.momiji.feature.command.brand.archive.ArchiveBrandCommand
import jp.momiji.feature.command.brand.archive.ArchiveBrandGrpcService
import jp.momiji.grpc.momiji.brand.archive.archiveBrandRequest
import kotlinx.coroutines.runBlocking
import org.axonframework.messaging.commandhandling.gateway.CommandGateway
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.concurrent.CompletableFuture

class ArchiveBrandGrpcServiceTest {
    private val commandGateway = mockk<CommandGateway>()
    private val service = ArchiveBrandGrpcService(commandGateway)

    private val validUlid = "01ARZ3NDEKTSV4RRFFQ69G5FAV"

    @Test
    fun `正常系_期待した Command が CommandGateway に渡る`() {
        every { commandGateway.send(any(), CommandResult::class.java) } returns
            CompletableFuture.completedFuture(CommandResult.success())

        runBlocking {
            service.archiveBrand(archiveBrandRequest { id = validUlid })
        }

        verify(exactly = 1) {
            commandGateway.send(
                match<ArchiveBrandCommand> { it.id == validUlid },
                CommandResult::class.java,
            )
        }
    }

    @Test
    fun `異常系_idがULID形式でないならValidationExceptionでCommandは流れない`() {
        assertThrows<ValidationException> {
            runBlocking {
                service.archiveBrand(archiveBrandRequest { id = "not-a-ulid" })
            }
        }

        verify(exactly = 0) { commandGateway.send(any(), CommandResult::class.java) }
    }
}
