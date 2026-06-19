package jp.momiji.feature.command.stock.adjust

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jp.momiji.domain.ValidationException
import jp.momiji.domain.stock.StockAdjustmentReason
import jp.momiji.feature.command.CommandResult
import jp.momiji.grpc.momiji.stock.adjust.adjustStockRequest
import kotlinx.coroutines.runBlocking
import org.axonframework.messaging.commandhandling.gateway.CommandGateway
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.concurrent.CompletableFuture
import jp.momiji.grpc.momiji.stock.StockAdjustmentReason as ProtoStockAdjustmentReason

class AdjustStockGrpcServiceTest {
    private val commandGateway = mockk<CommandGateway>()
    private val service = AdjustStockGrpcService(commandGateway)

    private val validUlid = "01ARZ3NDEKTSV4RRFFQ69G5FAV"

    @Test
    fun `正常系_期待した Command が CommandGateway に渡る`() {
        every { commandGateway.send(any(), CommandResult::class.java) } returns
            CompletableFuture.completedFuture(CommandResult.success())

        runBlocking {
            service.adjustStock(
                adjustStockRequest {
                    productId = validUlid
                    quantity = -3
                    reason = ProtoStockAdjustmentReason.STOCK_ADJUSTMENT_REASON_DAMAGED
                },
            )
        }

        verify(exactly = 1) {
            commandGateway.send(
                match<AdjustStockCommand> {
                    it.productId == validUlid &&
                        it.adjustment.quantity.value == -3 &&
                        it.adjustment.reason == StockAdjustmentReason.DAMAGED
                },
                CommandResult::class.java,
            )
        }
    }

    @Test
    fun `異常系_棚卸し以外で増加方向の調整はValidationExceptionでCommandは流れない`() {
        assertThrows<ValidationException> {
            runBlocking {
                service.adjustStock(
                    adjustStockRequest {
                        productId = validUlid
                        quantity = 5
                        reason = ProtoStockAdjustmentReason.STOCK_ADJUSTMENT_REASON_DAMAGED
                    },
                )
            }
        }

        verify(exactly = 0) { commandGateway.send(any(), CommandResult::class.java) }
    }

    @Test
    fun `正常系_棚卸しなら増加方向の調整が通る`() {
        every { commandGateway.send(any(), CommandResult::class.java) } returns
            CompletableFuture.completedFuture(CommandResult.success())

        runBlocking {
            service.adjustStock(
                adjustStockRequest {
                    productId = validUlid
                    quantity = 5
                    reason = ProtoStockAdjustmentReason.STOCK_ADJUSTMENT_REASON_STOCKTAKING
                },
            )
        }

        verify(exactly = 1) {
            commandGateway.send(
                match<AdjustStockCommand> {
                    it.adjustment.quantity.value == 5 &&
                        it.adjustment.reason == StockAdjustmentReason.STOCKTAKING
                },
                CommandResult::class.java,
            )
        }
    }

    @Test
    fun `異常系_productIdがULID形式でないならValidationExceptionでCommandは流れない`() {
        assertThrows<ValidationException> {
            runBlocking {
                service.adjustStock(
                    adjustStockRequest {
                        productId = "not-a-ulid"
                        quantity = 5
                        reason = ProtoStockAdjustmentReason.STOCK_ADJUSTMENT_REASON_CORRECTION
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
                service.adjustStock(
                    adjustStockRequest {
                        productId = validUlid
                        quantity = 0
                        reason = ProtoStockAdjustmentReason.STOCK_ADJUSTMENT_REASON_CORRECTION
                    },
                )
            }
        }

        verify(exactly = 0) { commandGateway.send(any(), CommandResult::class.java) }
    }

    @Test
    fun `異常系_reasonがUNSPECIFIEDならValidationExceptionでCommandは流れない`() {
        assertThrows<ValidationException> {
            runBlocking {
                service.adjustStock(
                    adjustStockRequest {
                        productId = validUlid
                        quantity = 5
                        reason = ProtoStockAdjustmentReason.STOCK_ADJUSTMENT_REASON_UNSPECIFIED
                    },
                )
            }
        }

        verify(exactly = 0) { commandGateway.send(any(), CommandResult::class.java) }
    }
}
