package jp.momiji.feature.command.product.update

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jp.momiji.domain.ValidationException
import jp.momiji.feature.command.CommandResult
import jp.momiji.feature.command.product.update.UpdateProductCommand
import jp.momiji.feature.command.product.update.UpdateProductGrpcService
import jp.momiji.grpc.momiji.product.update.v1.updateProductRequest
import kotlinx.coroutines.runBlocking
import org.axonframework.messaging.commandhandling.gateway.CommandGateway
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.concurrent.CompletableFuture

class UpdateProductGrpcServiceTest {
    private val commandGateway = mockk<CommandGateway>()
    private val service = UpdateProductGrpcService(commandGateway)

    private val validUlid = "01ARZ3NDEKTSV4RRFFQ69G5FAV"

    @Test
    fun `正常系_期待した Command が CommandGateway に渡る`() {
        every { commandGateway.send(any(), CommandResult::class.java) } returns
            CompletableFuture.completedFuture(CommandResult.success())

        runBlocking {
            service.updateProduct(
                updateProductRequest {
                    id = validUlid
                    name = "新商品名"
                    description = "新説明"
                    imageUrl = "https://example.com/new.png"
                    price = 2000
                },
            )
        }

        verify(exactly = 1) {
            commandGateway.send(
                match<UpdateProductCommand> {
                    it.id == validUlid &&
                        it.name.value == "新商品名" &&
                        it.description.value == "新説明" &&
                        it.imageUrl?.value == "https://example.com/new.png" &&
                        it.price.value == 2000
                },
                CommandResult::class.java,
            )
        }
    }

    @Test
    fun `異常系_idがULID形式でないならValidationExceptionでCommandは流れない`() {
        assertThrows<ValidationException> {
            runBlocking {
                service.updateProduct(
                    updateProductRequest {
                        id = "not-a-ulid"
                        name = "新商品名"
                        description = "新説明"
                        price = 2000
                    },
                )
            }
        }

        verify(exactly = 0) { commandGateway.send(any(), CommandResult::class.java) }
    }

    @Test
    fun `異常系_価格が範囲外ならValidationExceptionでCommandは流れない`() {
        assertThrows<ValidationException> {
            runBlocking {
                service.updateProduct(
                    updateProductRequest {
                        id = validUlid
                        name = "新商品名"
                        description = "新説明"
                        price = 0
                    },
                )
            }
        }

        verify(exactly = 0) { commandGateway.send(any(), CommandResult::class.java) }
    }
}
