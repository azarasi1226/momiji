package jp.momiji.feature.command.user.changeemail.request

import io.grpc.Context
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jp.momiji.config.grpc.GrpcAuthContext
import jp.momiji.domain.ValidationException
import jp.momiji.domain.user.Email
import jp.momiji.feature.command.CommandResult
import jp.momiji.feature.command.UserIdResolver
import jp.momiji.feature.command.user.changeemail.request.RequestEmailChangeCommand
import jp.momiji.feature.command.user.changeemail.request.RequestEmailChangeGrpcService
import jp.momiji.grpc.momiji.user.changeemail.request.v1.requestEmailChangeRequest
import kotlinx.coroutines.runBlocking
import org.axonframework.messaging.commandhandling.gateway.CommandGateway
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import java.util.concurrent.CompletableFuture
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RequestEmailChangeGrpcServiceTest {
    private val commandGateway = mockk<CommandGateway>()
    private val userIdResolver = mockk<UserIdResolver>()
    private val service = RequestEmailChangeGrpcService(commandGateway, userIdResolver)

    private val mockAccessToken = mockk<Jwt>()
    private val mockJwt = mockk<JwtAuthenticationToken> { every { token } returns mockAccessToken }

    private fun callRequestEmailChange(newEmail: String) =
        Context
            .current()
            .withValue(GrpcAuthContext.AUTH_KEY, mockJwt)
            .call {
                runBlocking {
                    service.requestEmailChange(
                        requestEmailChangeRequest { this.newEmail = newEmail },
                    )
                }
            }

    @Test
    fun `正常系_妥当な email なら期待した Command が CommandGateway に渡る`() {
        every { userIdResolver.resolve(mockAccessToken) } returns "test-user-id"
        every { commandGateway.send(any(), CommandResult::class.java) } returns
            CompletableFuture.completedFuture(CommandResult.success())

        callRequestEmailChange("new@example.com")

        verify(exactly = 1) {
            commandGateway.send(
                match<RequestEmailChangeCommand> {
                    it.userId == "test-user-id" && it.newEmail.value == "new@example.com"
                },
                CommandResult::class.java,
            )
        }
    }

    @Test
    fun `異常系_email 形式不正で ValidationException 投げ CommandGateway は呼ばれない`() {
        every { userIdResolver.resolve(mockAccessToken) } returns "test-user-id"

        val ex =
            assertThrows<ValidationException> {
                callRequestEmailChange("notanemail")
            }

        assertEquals(1, ex.errors.size)
        assertTrue(ex.errors.contains(Email.Invalid))

        verify(exactly = 0) { commandGateway.send(any(), any<Class<*>>()) }
    }
}
