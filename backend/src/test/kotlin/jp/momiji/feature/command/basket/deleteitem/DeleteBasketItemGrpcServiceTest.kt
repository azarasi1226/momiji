package jp.momiji.feature.command.basket.deleteitem

import io.grpc.Context
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jp.momiji.config.grpc.GrpcAuthContext
import jp.momiji.domain.ValidationException
import jp.momiji.feature.command.CommandResult
import jp.momiji.feature.command.UserIdResolver
import jp.momiji.grpc.momiji.basket.deleteitem.deleteBasketItemRequest
import kotlinx.coroutines.runBlocking
import org.axonframework.messaging.commandhandling.gateway.CommandGateway
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import java.util.concurrent.CompletableFuture

class DeleteBasketItemGrpcServiceTest {
    private val commandGateway = mockk<CommandGateway>()
    private val userIdResolver = mockk<UserIdResolver>()
    private val service = DeleteBasketItemGrpcService(commandGateway, userIdResolver)

    private val validUlid = "01ARZ3NDEKTSV4RRFFQ69G5FAV"
    private val mockAccessToken = mockk<Jwt>()
    private val mockJwt = mockk<JwtAuthenticationToken> { every { token } returns mockAccessToken }

    private fun <T> withAuth(block: () -> T): T =
        Context
            .current()
            .withValue(GrpcAuthContext.AUTH_KEY, mockJwt)
            .call(block)

    @Test
    fun `正常系_JWT解決したuserIdで期待した Command が CommandGateway に渡る`() {
        every { userIdResolver.resolve(mockAccessToken) } returns "test-user-id"
        every { commandGateway.send(any(), CommandResult::class.java) } returns
            CompletableFuture.completedFuture(CommandResult.success())

        withAuth {
            runBlocking {
                service.deleteBasketItem(deleteBasketItemRequest { productId = validUlid })
            }
        }

        verify(exactly = 1) {
            commandGateway.send(
                match<DeleteBasketItemCommand> { it.userId == "test-user-id" && it.productId == validUlid },
                CommandResult::class.java,
            )
        }
    }

    @Test
    fun `異常系_productIdがULID形式でなければValidationExceptionでCommandは流れない`() {
        every { userIdResolver.resolve(mockAccessToken) } returns "test-user-id"

        assertThrows<ValidationException> {
            withAuth {
                runBlocking {
                    service.deleteBasketItem(deleteBasketItemRequest { productId = "not-a-ulid" })
                }
            }
        }

        verify(exactly = 0) { commandGateway.send(any(), CommandResult::class.java) }
    }
}
