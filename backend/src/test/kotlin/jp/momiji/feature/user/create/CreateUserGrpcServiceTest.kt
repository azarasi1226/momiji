package jp.momiji.feature.user.create

import io.grpc.Context
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jp.momiji.config.grpc.GrpcAuthContext
import jp.momiji.domain.ValidationException
import jp.momiji.domain.idp.IdentityProvider
import jp.momiji.domain.user.Email
import jp.momiji.feature.CommandResult
import jp.momiji.feature.idp.IdpUserClient
import jp.momiji.grpc.momiji.user.create.v1.createUserRequest
import kotlinx.coroutines.runBlocking
import org.axonframework.messaging.commandhandling.gateway.CommandGateway
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import java.util.concurrent.CompletableFuture
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * CreateUserGrpcService の単体テスト。
 *
 * `gRPC リクエストには値が無く、 OidcUserInfoFetcher が IDP から取得した値で Command を組み立てる`
 * 構造なので、 validation 失敗の発生源は **broken IDP が変な email を返す** ケースのみ。
 * 内部の信頼境界ではあるが、 値オブジェクトを通すことで防御を入れている。
 */
class CreateUserGrpcServiceTest {
    private val oidcUserInfoFetcher = mockk<OidcUserInfoFetcher>()
    private val idpUserClient = mockk<IdpUserClient>()
    private val commandGateway = mockk<CommandGateway>()
    private val service = CreateUserGrpcService(oidcUserInfoFetcher, idpUserClient, commandGateway)

    private val mockJwt =
        mockk<JwtAuthenticationToken> {
            every { token } returns
                mockk<Jwt> {
                    every { tokenValue } returns "dummy-access-token"
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
    fun `正常系_IDP 取得の email が妥当なら期待した Command が CommandGateway に渡る`() {
        every { oidcUserInfoFetcher.handle("dummy-access-token") } returns
            OidcUserInfo(
                issuer = "https://idp.example.com",
                subject = "subj-1",
                email = "alice@example.com",
                emailVerified = true,
            )
        every { idpUserClient.resolveIdentityProvider("dummy-access-token") } returns IdentityProvider.LOCAL
        every { commandGateway.send(any(), CommandResult::class.java) } returns
            CompletableFuture.completedFuture(CommandResult.success())

        callCreateUser()

        verify(exactly = 1) {
            commandGateway.send(
                match<CreateUserCommand> {
                    it.oidcIssuer == "https://idp.example.com" &&
                        it.oidcSubject == "subj-1" &&
                        it.oidcIdentityProvider == IdentityProvider.LOCAL &&
                        it.email.value == "alice@example.com" &&
                        it.emailVerified
                },
                CommandResult::class.java,
            )
        }
    }

    @Test
    fun `異常系_IDP が形式不正の email を返したら ValidationException`() {
        every { oidcUserInfoFetcher.handle("dummy-access-token") } returns
            OidcUserInfo(
                issuer = "https://idp.example.com",
                subject = "subj-1",
                email = "notanemail",
                emailVerified = true,
            )
        every { idpUserClient.resolveIdentityProvider("dummy-access-token") } returns IdentityProvider.LOCAL

        val ex = assertThrows<ValidationException> { callCreateUser() }
        assertEquals(1, ex.errors.size)
        assertTrue(ex.errors.contains(Email.Invalid))

        verify(exactly = 0) { commandGateway.send(any(), any<Class<*>>()) }
    }
}
