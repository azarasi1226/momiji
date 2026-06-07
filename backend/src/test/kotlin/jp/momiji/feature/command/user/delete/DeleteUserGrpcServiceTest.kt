package jp.momiji.feature.command.user.delete

import io.grpc.Context
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jp.momiji.config.grpc.GrpcAuthContext
import jp.momiji.feature.command.CommandResult
import jp.momiji.feature.command.UserIdResolver
import jp.momiji.feature.command.user.delete.DeleteUserCommand
import jp.momiji.feature.command.user.delete.DeleteUserGrpcService
import jp.momiji.grpc.momiji.user.delete.v1.deleteUserRequest
import kotlinx.coroutines.runBlocking
import org.axonframework.messaging.commandhandling.gateway.CommandGateway
import org.junit.jupiter.api.Test
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import java.util.concurrent.CompletableFuture

class DeleteUserGrpcServiceTest {
    private val commandGateway = mockk<CommandGateway>()
    private val userIdResolver = mockk<UserIdResolver>()
    private val service = DeleteUserGrpcService(commandGateway, userIdResolver)

    private val mockAccessToken = mockk<Jwt>()
    private val mockJwt = mockk<JwtAuthenticationToken> { every { token } returns mockAccessToken }

    @Test
    fun `正常系_期待した Command が CommandGateway に渡る`() {
        every { userIdResolver.resolve(mockAccessToken) } returns "test-user-id"
        every { commandGateway.send(any(), CommandResult::class.java) } returns
            CompletableFuture.completedFuture(CommandResult.success())

        Context
            .current()
            .withValue(GrpcAuthContext.AUTH_KEY, mockJwt)
            .call {
                runBlocking {
                    service.deleteUser(deleteUserRequest { })
                }
            }

        verify(exactly = 1) {
            commandGateway.send(
                match<DeleteUserCommand> { it.id == "test-user-id" },
                CommandResult::class.java,
            )
        }
    }
}
