package jp.momiji.feature.command.order.cancel

import io.grpc.Context
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import jp.momiji.config.grpc.GrpcAuthContext
import jp.momiji.domain.BusinessException
import jp.momiji.domain.order.OrderCancellationReason
import jp.momiji.feature.command.CommandResult
import jp.momiji.feature.command.UserIdResolver
import jp.momiji.feature.command.order.OrderOwnershipReader
import jp.momiji.feature.command.order.OrderProductIdsReader
import jp.momiji.grpc.momiji.order.cancel.CancellationReason
import jp.momiji.grpc.momiji.order.cancel.cancelOrderRequest
import kotlinx.coroutines.runBlocking
import org.axonframework.messaging.commandhandling.gateway.CommandGateway
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import java.util.concurrent.CompletableFuture

class CancelOrderGrpcServiceTest {
    private val commandGateway = mockk<CommandGateway>()
    private val userIdResolver = mockk<UserIdResolver>()
    private val orderOwnershipReader = mockk<OrderOwnershipReader>()
    private val orderProductIdsReader = mockk<OrderProductIdsReader>()
    private val service =
        CancelOrderGrpcService(commandGateway, userIdResolver, orderOwnershipReader, orderProductIdsReader)

    private val mockAccessToken = mockk<Jwt>()
    private val mockJwt = mockk<JwtAuthenticationToken> { every { token } returns mockAccessToken }

    private fun <T> withAuth(block: () -> T): T =
        Context
            .current()
            .withValue(GrpcAuthContext.AUTH_KEY, mockJwt)
            .call(block)

    @Test
    fun `本人の注文なら理由を変換し productIds を載せて CancelOrder を撃つ`() {
        every { userIdResolver.resolve(mockAccessToken) } returns "user-1"
        every { orderOwnershipReader.isOwnedBy("order-1", "user-1") } returns true
        every { orderProductIdsReader.read("order-1") } returns listOf("prod-1", "prod-2")
        val cmdSlot = slot<CancelOrderCommand>()
        every { commandGateway.send(capture(cmdSlot), CommandResult::class.java) } returns
            CompletableFuture.completedFuture(CommandResult.success())

        withAuth {
            runBlocking {
                service.cancelOrder(
                    cancelOrderRequest {
                        orderId = "order-1"
                        reason = CancellationReason.CANCELLATION_REASON_CHANGED_MIND
                    },
                )
            }
        }

        val sent = cmdSlot.captured
        assertEquals("order-1", sent.orderId)
        assertEquals(listOf("prod-1", "prod-2"), sent.productIds)
        assertEquals(OrderCancellationReason.CHANGED_MIND, sent.reason)
    }

    @Test
    fun `本人の注文でなければ BusinessException でコマンドを撃たない`() {
        every { userIdResolver.resolve(mockAccessToken) } returns "user-2"
        every { orderOwnershipReader.isOwnedBy("order-x", "user-2") } returns false

        assertThrows<BusinessException> {
            withAuth {
                runBlocking {
                    service.cancelOrder(
                        cancelOrderRequest {
                            orderId = "order-x"
                            reason = CancellationReason.CANCELLATION_REASON_CHANGED_MIND
                        },
                    )
                }
            }
        }
        verify(exactly = 0) { commandGateway.send(any(), CommandResult::class.java) }
    }

    @Test
    fun `理由が未選択（UNSPECIFIED）なら BusinessException でコマンドを撃たない`() {
        every { userIdResolver.resolve(mockAccessToken) } returns "user-3"
        every { orderOwnershipReader.isOwnedBy("order-3", "user-3") } returns true
        every { orderProductIdsReader.read("order-3") } returns listOf("prod-3")

        assertThrows<BusinessException> {
            withAuth {
                runBlocking {
                    service.cancelOrder(
                        cancelOrderRequest {
                            orderId = "order-3"
                            reason = CancellationReason.CANCELLATION_REASON_UNSPECIFIED
                        },
                    )
                }
            }
        }
        verify(exactly = 0) { commandGateway.send(any(), CommandResult::class.java) }
    }

    @Test
    fun `コマンドが業務エラー（発送済み等）なら BusinessException`() {
        every { userIdResolver.resolve(mockAccessToken) } returns "user-4"
        every { orderOwnershipReader.isOwnedBy("order-4", "user-4") } returns true
        every { orderProductIdsReader.read("order-4") } returns listOf("prod-4")
        every { commandGateway.send(any(), CommandResult::class.java) } returns
            CompletableFuture.completedFuture(CancelOrderCommandResult.alreadyShipped())

        assertThrows<BusinessException> {
            withAuth {
                runBlocking {
                    service.cancelOrder(
                        cancelOrderRequest {
                            orderId = "order-4"
                            reason = CancellationReason.CANCELLATION_REASON_OTHER
                        },
                    )
                }
            }
        }
    }
}
