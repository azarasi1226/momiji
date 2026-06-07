package jp.momiji.feature.command.user.changeemail.confirm

import io.grpc.Context
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jp.momiji.config.grpc.GrpcAuthContext
import jp.momiji.domain.ValidationException
import jp.momiji.domain.user.EmailChangeToken
import jp.momiji.feature.command.CommandResult
import jp.momiji.feature.command.user.UserIdResolver
import jp.momiji.feature.command.user.changeemail.confirm.ConfirmEmailChangeCommand
import jp.momiji.feature.command.user.changeemail.confirm.ConfirmEmailChangeGrpcService
import jp.momiji.grpc.momiji.user.changeemail.confirm.v1.confirmEmailChangeRequest
import kotlinx.coroutines.runBlocking
import org.axonframework.messaging.commandhandling.gateway.CommandGateway
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import java.util.concurrent.CompletableFuture
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ConfirmEmailChangeGrpcServiceTest {
    private val commandGateway = mockk<CommandGateway>()
    private val userIdResolver = mockk<UserIdResolver>()
    private val service = ConfirmEmailChangeGrpcService(commandGateway, userIdResolver)

    private val mockAccessToken = mockk<Jwt>()
    private val mockJwt = mockk<JwtAuthenticationToken> { every { token } returns mockAccessToken }

    private fun callConfirmEmailChange(token: String) =
        Context
            .current()
            .withValue(GrpcAuthContext.AUTH_KEY, mockJwt)
            .call {
                runBlocking {
                    service.confirmEmailChange(
                        confirmEmailChangeRequest { this.token = token },
                    )
                }
            }

    @Test
    fun `正常系_JWT 風 3 セグメントなら期待した Command が CommandGateway に渡る`() {
        every { userIdResolver.resolve(mockAccessToken) } returns "test-user-id"
        every { commandGateway.send(any(), CommandResult::class.java) } returns
            CompletableFuture.completedFuture(CommandResult.success())

        val validToken = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0ZXN0In0.signature"
        callConfirmEmailChange(validToken)

        verify(exactly = 1) {
            commandGateway.send(
                match<ConfirmEmailChangeCommand> {
                    it.userId == "test-user-id" && it.token.value == validToken
                },
                CommandResult::class.java,
            )
        }
    }

    @Test
    fun `異常系_token 形式不正で ValidationException 投げ CommandGateway は呼ばれない`() {
        every { userIdResolver.resolve(mockAccessToken) } returns "test-user-id"

        val ex =
            assertThrows<ValidationException> {
                callConfirmEmailChange("notajwt")
            }

        assertEquals(1, ex.errors.size)
        assertTrue(ex.errors.contains(EmailChangeToken.InvalidFormat))

        verify(exactly = 0) { commandGateway.send(any(), any<Class<*>>()) }
    }
}
