package jp.momiji.feature.command.user.create

import com.github.michaelbull.result.get
import io.grpc.Context
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jp.momiji.config.grpc.GrpcAuthContext
import jp.momiji.domain.idp.IdentityProvider
import jp.momiji.domain.user.Email
import jp.momiji.feature.command.CommandResult
import jp.momiji.feature.command.user.create.CreateUserCommand
import jp.momiji.feature.command.user.create.CreateUserGrpcService
import jp.momiji.grpc.momiji.user.create.v1.createUserRequest
import jp.momiji.port.idp.IdpUserInfoFetcher
import jp.momiji.port.idp.OidcUserInfo
import kotlinx.coroutines.runBlocking
import org.axonframework.messaging.commandhandling.gateway.CommandGateway
import org.junit.jupiter.api.Test
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import java.net.URI
import java.util.concurrent.CompletableFuture

/**
 * CreateUserGrpcService の単体テスト。
 *
 * 検証済みトークンの subject / issuer を IdpUserInfoFetcher に渡すと identityProvider / email ( 値オブジェクト )
 * まで解決済みの OidcUserInfo が返るので、 このサービスは「完成品」 を Command に組み替えて流すだけ。
 * email の形式検証などは fetcher 側 ( [jp.momiji.adapter.idp.CognitoUserInfoFetcher] のテスト参照 )。
 */
class CreateUserGrpcServiceTest {
    private val idpUserInfoFetcher = mockk<IdpUserInfoFetcher>()
    private val commandGateway = mockk<CommandGateway>()
    private val service = CreateUserGrpcService(idpUserInfoFetcher, commandGateway)

    private val mockJwt =
        mockk<JwtAuthenticationToken> {
            every { token } returns
                mockk<Jwt> {
                    every { subject } returns "subj-1"
                    every { issuer } returns URI("https://idp.example.com").toURL()
                }
        }

    private fun callCreateUser() =
        Context
            .current()
            .withValue(GrpcAuthContext.AUTH_KEY, mockJwt)
            .call {
                runBlocking {
                    service.createUser(createUserRequest { })
                }
            }

    @Test
    fun `正常系_IdpUserInfoFetcher が返した値で期待した Command が CommandGateway に渡る`() {
        every { idpUserInfoFetcher.handle("subj-1", "https://idp.example.com") } returns
            OidcUserInfo(
                issuer = "https://idp.example.com",
                subject = "subj-1",
                email = Email.create("alice@example.com").get()!!,
                emailVerified = true,
                identityProvider = IdentityProvider.GOOGLE,
            )
        every { commandGateway.send(any(), CommandResult::class.java) } returns
            CompletableFuture.completedFuture(CommandResult.success())

        callCreateUser()

        verify(exactly = 1) {
            commandGateway.send(
                match<CreateUserCommand> {
                    it.oidcIssuer == "https://idp.example.com" &&
                        it.oidcSubject == "subj-1" &&
                        it.oidcIdentityProvider == IdentityProvider.GOOGLE &&
                        it.email.value == "alice@example.com" &&
                        it.emailVerified
                },
                CommandResult::class.java,
            )
        }
    }
}
