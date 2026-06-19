package jp.momiji.feature.command.order.start

import io.grpc.Context
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import jp.momiji.config.grpc.GrpcAuthContext
import jp.momiji.domain.BusinessError
import jp.momiji.domain.BusinessException
import jp.momiji.feature.command.CommandResult
import jp.momiji.feature.command.UserIdResolver
import jp.momiji.grpc.momiji.order.start.startOrderRequest
import kotlinx.coroutines.runBlocking
import org.axonframework.messaging.commandhandling.gateway.CommandGateway
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import java.util.concurrent.CompletableFuture

class StartOrderGrpcServiceTest {
    private val commandGateway = mockk<CommandGateway>()
    private val userIdResolver = mockk<UserIdResolver>()
    private val orderBasketReader = mockk<OrderBasketReader>()
    private val service = StartOrderGrpcService(commandGateway, userIdResolver, orderBasketReader)

    private val mockAccessToken = mockk<Jwt>()
    private val mockJwt = mockk<JwtAuthenticationToken> { every { token } returns mockAccessToken }

    private fun <T> withAuth(block: () -> T): T =
        Context
            .current()
            .withValue(GrpcAuthContext.AUTH_KEY, mockJwt)
            .call(block)

    private fun request(
        addressId: String,
        total: Long,
    ) = startOrderRequest {
        shippingAddressId = addressId
        expectedTotalAmount = total
    }

    @Test
    fun `カートの中身でStartOrderCommandを送り、採番したorderIdを返す`() {
        every { userIdResolver.resolve(mockAccessToken) } returns "user-1"
        every { orderBasketReader.readItems("user-1") } returns
            listOf(StartOrderCommand.Item("prod-a", 2), StartOrderCommand.Item("prod-b", 1))
        val cmdSlot = slot<StartOrderCommand>()
        every { commandGateway.send(capture(cmdSlot), CommandResult::class.java) } returns
            CompletableFuture.completedFuture(CommandResult.success())

        val response = withAuth { runBlocking { service.startOrder(request("addr-1", total = 2500)) } }

        // 採番した order id を返し、 それが送ったコマンドの id と一致する。
        assertTrue(response.orderId.isNotBlank())
        val sent = cmdSlot.captured
        assertEquals(response.orderId, sent.id)
        assertEquals("user-1", sent.userId)
        assertEquals("addr-1", sent.shippingAddressId)
        assertEquals(2500L, sent.expectedTotalAmount)
        assertEquals(listOf(StartOrderCommand.Item("prod-a", 2), StartOrderCommand.Item("prod-b", 1)), sent.items)
    }

    @Test
    fun `カートが空ならBusinessExceptionでコマンドを送らない`() {
        every { userIdResolver.resolve(mockAccessToken) } returns "user-2"
        every { orderBasketReader.readItems("user-2") } returns emptyList()

        assertThrows<BusinessException> {
            withAuth { runBlocking { service.startOrder(request("addr-2", total = 0)) } }
        }
        verify(exactly = 0) { commandGateway.send(any(), CommandResult::class.java) }
    }

    @Test
    fun `コマンドが業務エラーならBusinessExceptionになる`() {
        every { userIdResolver.resolve(mockAccessToken) } returns "user-3"
        every { orderBasketReader.readItems("user-3") } returns listOf(StartOrderCommand.Item("prod-c", 1))
        every { commandGateway.send(any(), CommandResult::class.java) } returns
            CompletableFuture.completedFuture(CommandResult.fail(BusinessError("金額が変わっています")))

        assertThrows<BusinessException> {
            withAuth { runBlocking { service.startOrder(request("addr-3", total = 999)) } }
        }
    }
}
