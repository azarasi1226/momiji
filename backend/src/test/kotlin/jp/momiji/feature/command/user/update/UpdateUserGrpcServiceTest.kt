package jp.momiji.feature.command.user.update

import io.grpc.Context
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jp.momiji.config.grpc.GrpcAuthContext
import jp.momiji.domain.ValidationException
import jp.momiji.feature.command.CommandResult
import jp.momiji.feature.command.UserIdResolver
import jp.momiji.grpc.momiji.user.update.v1.updateUserRequest
import kotlinx.coroutines.runBlocking
import org.axonframework.messaging.commandhandling.gateway.CommandGateway
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import java.util.concurrent.CompletableFuture

/**
 * [UpdateUserGrpcService] の単体テスト。
 *
 * プロフィールは name のみ（住所・電話は配送先ユースケースへ移管済み）。
 * GrpcAuthContext は io.grpc の thread-local Context に依存するので、
 * `Context.current().withValue(AUTH_KEY, mockJwt).call { runBlocking { ... } }` で bind する。
 */
class UpdateUserGrpcServiceTest {
    private val commandGateway = mockk<CommandGateway>()
    private val userIdResolver = mockk<UserIdResolver>()
    private val service = UpdateUserGrpcService(commandGateway, userIdResolver)

    private val mockAccessToken = mockk<Jwt>()
    private val mockJwt = mockk<JwtAuthenticationToken> { every { token } returns mockAccessToken }

    private fun callUpdateUser(name: String) =
        Context
            .current()
            .withValue(GrpcAuthContext.AUTH_KEY, mockJwt)
            .call {
                runBlocking {
                    service.updateUser(updateUserRequest { this.name = name })
                }
            }

    @Test
    fun `正常系_nameが妥当なら期待した Command が CommandGateway に渡る`() {
        every { userIdResolver.resolve(mockAccessToken) } returns "test-user-id"
        every { commandGateway.send(any(), CommandResult::class.java) } returns
            CompletableFuture.completedFuture(CommandResult.success())

        callUpdateUser("Alice")

        verify(exactly = 1) {
            commandGateway.send(
                match<UpdateUserCommand> {
                    it.id == "test-user-id" && it.name.value == "Alice"
                },
                CommandResult::class.java,
            )
        }
    }

    @Test
    fun `異常系_nameが空ならValidationExceptionでCommandは流れない`() {
        every { userIdResolver.resolve(mockAccessToken) } returns "test-user-id"

        assertThrows<ValidationException> {
            callUpdateUser("")
        }

        verify(exactly = 0) { commandGateway.send(any(), CommandResult::class.java) }
    }
}
