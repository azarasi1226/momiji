package jp.momiji.feature.user.findbyid

import io.grpc.Context
import io.mockk.every
import io.mockk.mockk
import jp.momiji.config.grpc.GrpcAuthContext
import jp.momiji.domain.UseCaseException
import jp.momiji.feature.user.UserIdResolver
import jp.momiji.grpc.momiji.user.findbyid.v1.findUserByIdRequest
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import java.time.LocalDateTime
import kotlin.test.assertEquals

class FindUserByIdGrpcServiceTest {
    private val userIdResolver = mockk<UserIdResolver>()
    private val findUserByIdQueryService = mockk<FindUserByIdQueryService>()
    private val service = FindUserByIdGrpcService(userIdResolver, findUserByIdQueryService)

    private val mockJwt = mockk<JwtAuthenticationToken>()

    private fun callFindUserById() =
        Context
            .current()
            .withValue(GrpcAuthContext.AUTH_KEY, mockJwt)
            .call {
                runBlocking {
                    service.findUserById(findUserByIdRequest { })
                }
            }

    @Test
    fun `正常系_ユーザーが存在すれば各フィールドを response に詰めて返す`() {
        every { userIdResolver.resolve(mockJwt) } returns "test-user-id"
        every { findUserByIdQueryService.findById("test-user-id") } returns
            UserView(
                id = "test-user-id",
                email = "alice@example.com",
                name = "Alice",
                phoneNumber = "090-0000-0000",
                postalCode = "100-0000",
                address1 = "東京都千代田区",
                address2 = "千代田 1-1",
                createdAt = LocalDateTime.parse("2026-01-01T00:00:00"),
                updatedAt = LocalDateTime.parse("2026-01-02T00:00:00"),
            )

        val response = callFindUserById()

        assertEquals("test-user-id", response.id)
        assertEquals("alice@example.com", response.email)
        assertEquals("Alice", response.name)
        assertEquals("090-0000-0000", response.phoneNumber)
        assertEquals("100-0000", response.postalCode)
        assertEquals("東京都千代田区", response.address1)
        assertEquals("千代田 1-1", response.address2)
    }

    @Test
    fun `異常系_ユーザーが見つからなければ UseCaseException`() {
        every { userIdResolver.resolve(mockJwt) } returns "test-user-id"
        every { findUserByIdQueryService.findById("test-user-id") } returns null

        val ex = assertThrows<UseCaseException> { callFindUserById() }
        assertEquals("ユーザーが見つかりません", ex.error.message)
    }
}
