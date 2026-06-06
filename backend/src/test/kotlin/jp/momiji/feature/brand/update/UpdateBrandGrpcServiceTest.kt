package jp.momiji.feature.brand.update

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jp.momiji.domain.ValidationException
import jp.momiji.feature.command.CommandResult
import jp.momiji.feature.command.brand.update.UpdateBrandCommand
import jp.momiji.feature.command.brand.update.UpdateBrandGrpcService
import jp.momiji.grpc.momiji.brand.update.v1.updateBrandRequest
import kotlinx.coroutines.runBlocking
import org.axonframework.messaging.commandhandling.gateway.CommandGateway
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.concurrent.CompletableFuture

class UpdateBrandGrpcServiceTest {
    private val commandGateway = mockk<CommandGateway>()
    private val service = UpdateBrandGrpcService(commandGateway)

    private val validUlid = "01ARZ3NDEKTSV4RRFFQ69G5FAV"

    @Test
    fun `正常系_期待した Command が CommandGateway に渡る`() {
        every { commandGateway.send(any(), CommandResult::class.java) } returns
            CompletableFuture.completedFuture(CommandResult.success())

        runBlocking {
            service.updateBrand(
                updateBrandRequest {
                    id = validUlid
                    name = "新ブランド名"
                    description = "新説明"
                },
            )
        }

        verify(exactly = 1) {
            commandGateway.send(
                match<UpdateBrandCommand> {
                    it.id == validUlid &&
                        it.name.value == "新ブランド名" &&
                        it.description.value == "新説明"
                },
                CommandResult::class.java,
            )
        }
    }

    @Test
    fun `異常系_idがULID形式でないならValidationExceptionでCommandは流れない`() {
        assertThrows<ValidationException> {
            runBlocking {
                service.updateBrand(
                    updateBrandRequest {
                        id = "not-a-ulid"
                        name = "新ブランド名"
                        description = "新説明"
                    },
                )
            }
        }

        verify(exactly = 0) { commandGateway.send(any(), CommandResult::class.java) }
    }

    @Test
    fun `異常系_名前が空ならValidationExceptionでCommandは流れない`() {
        assertThrows<ValidationException> {
            runBlocking {
                service.updateBrand(
                    updateBrandRequest {
                        id = validUlid
                        name = ""
                        description = "説明"
                    },
                )
            }
        }

        verify(exactly = 0) { commandGateway.send(any(), CommandResult::class.java) }
    }
}
