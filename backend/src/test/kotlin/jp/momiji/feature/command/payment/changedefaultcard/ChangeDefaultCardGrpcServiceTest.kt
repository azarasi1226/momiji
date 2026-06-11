package jp.momiji.feature.command.payment.changedefaultcard

import io.grpc.Context
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jp.momiji.config.grpc.GrpcAuthContext
import jp.momiji.domain.BusinessError
import jp.momiji.domain.BusinessException
import jp.momiji.feature.command.CommandResult
import jp.momiji.feature.command.UserIdResolver
import jp.momiji.grpc.momiji.payment.changedefaultcard.v1.changeDefaultCardRequest
import kotlinx.coroutines.runBlocking
import org.axonframework.messaging.commandhandling.gateway.CommandGateway
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import java.util.concurrent.CompletableFuture

class ChangeDefaultCardGrpcServiceTest {
    private val commandGateway = mockk<CommandGateway>()
    private val userIdResolver = mockk<UserIdResolver>()
    private val service = ChangeDefaultCardGrpcService(commandGateway, userIdResolver)

    private val mockAccessToken = mockk<Jwt>()
    private val mockJwt = mockk<JwtAuthenticationToken> { every { token } returns mockAccessToken }

    private fun <T> withAuth(block: () -> T): T =
        Context
            .current()
            .withValue(GrpcAuthContext.AUTH_KEY, mockJwt)
            .call(block)

    @Test
    fun `正常系_JWT解決したuserIdとリクエストのpmで期待したCommandがCommandGatewayに渡る`() {
        every { userIdResolver.resolve(mockAccessToken) } returns "user-1"
        every { commandGateway.send(any(), CommandResult::class.java) } returns
            CompletableFuture.completedFuture(CommandResult.success())

        withAuth {
            runBlocking { service.changeDefaultCard(changeDefaultCardRequest { paymentMethodId = "pm_def_1" }) }
        }

        verify(exactly = 1) {
            commandGateway.send(
                match<ChangeDefaultCardCommand> { it.userId == "user-1" && it.paymentMethodId == "pm_def_1" },
                CommandResult::class.java,
            )
        }
    }

    @Test
    fun `異常系_Commandが業務エラーならBusinessExceptionになる`() {
        every { userIdResolver.resolve(mockAccessToken) } returns "user-2"
        every { commandGateway.send(any(), CommandResult::class.java) } returns
            CompletableFuture.completedFuture(CommandResult.fail(BusinessError("カードが存在しません")))

        assertThrows<BusinessException> {
            withAuth {
                runBlocking { service.changeDefaultCard(changeDefaultCardRequest { paymentMethodId = "pm_def_2" }) }
            }
        }
    }
}
