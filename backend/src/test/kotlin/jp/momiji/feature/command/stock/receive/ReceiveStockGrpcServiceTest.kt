package jp.momiji.feature.command.stock.receive

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jp.momiji.domain.ValidationException
import jp.momiji.feature.command.CommandResult
import jp.momiji.grpc.momiji.stock.receive.receiveStockRequest
import kotlinx.coroutines.runBlocking
import org.axonframework.messaging.commandhandling.gateway.CommandGateway
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.concurrent.CompletableFuture

class ReceiveStockGrpcServiceTest {
    private val commandGateway = mockk<CommandGateway>()
    private val service = ReceiveStockGrpcService(commandGateway)

    private val validUlid = "01ARZ3NDEKTSV4RRFFQ69G5FAV"

    @Test
    fun `正常系_期待した Command が CommandGateway に渡る`() {
        every { commandGateway.send(any(), CommandResult::class.java) } returns
            CompletableFuture.completedFuture(CommandResult.success())

        runBlocking {
            service.receiveStock(
                receiveStockRequest {
                    productId = validUlid
                    quantity = 10
                },
            )
        }

        verify(exactly = 1) {
            commandGateway.send(
                match<ReceiveStockCommand> {
                    it.productId == validUlid && it.receivedQuantity.value == 10
                },
                CommandResult::class.java,
            )
        }
    }

    @Test
    fun `異常系_productIdがULID形式でないならValidationExceptionでCommandは流れない`() {
        assertThrows<ValidationException> {
            runBlocking {
                service.receiveStock(
                    receiveStockRequest {
                        productId = "not-a-ulid"
                        quantity = 10
                    },
                )
            }
        }

        verify(exactly = 0) { commandGateway.send(any(), CommandResult::class.java) }
    }

    @Test
    fun `異常系_quantityが0ならValidationExceptionでCommandは流れない`() {
        assertThrows<ValidationException> {
            runBlocking {
                service.receiveStock(
                    receiveStockRequest {
                        productId = validUlid
                        quantity = 0
                    },
                )
            }
        }

        verify(exactly = 0) { commandGateway.send(any(), CommandResult::class.java) }
    }
}
