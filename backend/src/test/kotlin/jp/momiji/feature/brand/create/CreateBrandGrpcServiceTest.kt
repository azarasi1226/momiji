package jp.momiji.feature.brand.create

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jp.momiji.domain.ValidationException
import jp.momiji.feature.command.CommandResult
import jp.momiji.feature.command.brand.create.CreateBrandCommand
import jp.momiji.feature.command.brand.create.CreateBrandGrpcService
import jp.momiji.grpc.momiji.brand.create.v1.createBrandRequest
import kotlinx.coroutines.runBlocking
import org.axonframework.messaging.commandhandling.gateway.CommandGateway
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.concurrent.CompletableFuture
import kotlin.test.assertEquals

class CreateBrandGrpcServiceTest {
    private val commandGateway = mockk<CommandGateway>()
    private val service = CreateBrandGrpcService(commandGateway)

    private val validUlid = "01ARZ3NDEKTSV4RRFFQ69G5FAV"

    @Test
    fun `正常系_渡されたIDで期待した Command が CommandGateway に渡りIDを返す`() {
        every { commandGateway.send(any(), CommandResult::class.java) } returns
            CompletableFuture.completedFuture(CommandResult.success())

        val response =
            runBlocking {
                service.createBrand(
                    createBrandRequest {
                        id = validUlid
                        name = "テストブランド"
                        description = "テスト説明"
                    },
                )
            }

        assertEquals(validUlid, response.id)
        verify(exactly = 1) {
            commandGateway.send(
                match<CreateBrandCommand> {
                    it.id == validUlid &&
                        it.name.value == "テストブランド" &&
                        it.description.value == "テスト説明"
                },
                CommandResult::class.java,
            )
        }
    }

    @Test
    fun `異常系_idがULID形式でないならValidationExceptionでCommandは流れない`() {
        assertThrows<ValidationException> {
            runBlocking {
                service.createBrand(
                    createBrandRequest {
                        id = "not-a-ulid"
                        name = "テストブランド"
                        description = "テスト説明"
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
                service.createBrand(
                    createBrandRequest {
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
