package jp.momiji.feature.user.update

import io.grpc.Context
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jp.momiji.config.grpc.GrpcAuthContext
import jp.momiji.domain.ValidationException
import jp.momiji.domain.user.Address1
import jp.momiji.domain.user.Name
import jp.momiji.domain.user.PhoneNumber
import jp.momiji.domain.user.PostalCode
import jp.momiji.feature.CommandResult
import jp.momiji.feature.user.UserIdResolver
import jp.momiji.grpc.momiji.user.update.v1.updateUserRequest
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
 * [UpdateUserGrpcService] の単体テスト。
 *
 * 統合テスト ([UpdateUserCommandHandlerTest]) が「CommandHandler 以降 → EventStore + projection」 を担うので、
 * ここでは GrpcService 固有の責務だけを 2 ケースで cover する:
 * - 正常系: 値オブジェクト validation が通り、 期待した Command が CommandGateway に渡る
 * - 異常系: validation 失敗で ValidationException が投げられ、 CommandGateway は呼ばれない
 *
 * GrpcAuthContext は io.grpc の thread-local Context に依存するので、
 * `Context.current().withValue(AUTH_KEY, mockJwt).call { runBlocking { ... } }` で bind する。
 */
class UpdateUserGrpcServiceTest {
    private val commandGateway = mockk<CommandGateway>()
    private val userIdResolver = mockk<UserIdResolver>()
    private val service = UpdateUserGrpcService(commandGateway, userIdResolver)

    private val mockAccessToken = mockk<Jwt>()
    private val mockJwt = mockk<JwtAuthenticationToken> { every { token } returns mockAccessToken }

    private fun callUpdateUser(
        name: String,
        phoneNumber: String,
        postalCode: String,
        address1: String,
        address2: String,
    ) = Context
        .current()
        .withValue(GrpcAuthContext.AUTH_KEY, mockJwt)
        .call {
            runBlocking {
                service.updateUser(
                    updateUserRequest {
                        this.name = name
                        this.phoneNumber = phoneNumber
                        this.postalCode = postalCode
                        this.address1 = address1
                        this.address2 = address2
                    },
                )
            }
        }

    @Test
    fun `正常系_全フィールド妥当なら期待した Command が CommandGateway に渡る`() {
        every { userIdResolver.resolve(mockAccessToken) } returns "test-user-id"
        every { commandGateway.send(any(), CommandResult::class.java) } returns
            CompletableFuture.completedFuture(CommandResult.success())

        callUpdateUser("Alice", "090-0000-0000", "100-0000", "東京都千代田区", "千代田 1-1")

        verify(exactly = 1) {
            commandGateway.send(
                match<UpdateUserCommand> {
                    it.id == "test-user-id" &&
                        it.name.value == "Alice" &&
                        it.phoneNumber.value == "090-0000-0000" &&
                        it.postalCode.value == "100-0000" &&
                        it.address1.value == "東京都千代田区" &&
                        it.address2.value == "千代田 1-1"
                },
                CommandResult::class.java,
            )
        }
    }

    @Test
    fun `異常系_validation 失敗で ValidationException 投げ CommandGateway は呼ばれない`() {
        every { userIdResolver.resolve(mockAccessToken) } returns "test-user-id"

        val ex =
            assertThrows<ValidationException> {
                // 名前空 + 電話不正 + 郵便不正 + 住所1空 (4 フィールド同時失敗)
                callUpdateUser("", "invalid", "", "", "OK")
            }

        // 4 エラーが集約されている
        assertEquals(4, ex.errors.size)
        assertTrue(ex.errors.contains(Name.Blank))
        assertTrue(ex.errors.contains(PhoneNumber.Invalid))
        assertTrue(ex.errors.contains(PostalCode.Invalid))
        assertTrue(ex.errors.contains(Address1.Blank))

        verify(exactly = 0) { commandGateway.send(any(), any<Class<*>>()) }
    }
}
