package jp.momiji.feature.query.basket.findmybasketsummary

import io.grpc.Context
import io.mockk.every
import io.mockk.mockk
import jp.momiji.config.grpc.GrpcAuthContext
import jp.momiji.feature.command.UserIdResolver
import jp.momiji.grpc.momiji.basket.findmybasketsummary.findMyBasketSummaryRequest
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken

class FindMyBasketSummaryGrpcServiceTest {
    private val userIdResolver = mockk<UserIdResolver>()
    private val findMyBasketSummaryQueryService = mockk<FindMyBasketSummaryQueryService>()
    private val service = FindMyBasketSummaryGrpcService(userIdResolver, findMyBasketSummaryQueryService)

    private val mockAccessToken = mockk<Jwt>()
    private val mockJwt = mockk<JwtAuthenticationToken> { every { token } returns mockAccessToken }

    private fun <T> withAuth(block: () -> T): T =
        Context
            .current()
            .withValue(GrpcAuthContext.AUTH_KEY, mockJwt)
            .call(block)

    @Test
    fun `本人のカゴ集計（合計数量・種類数・合計金額）が proto へマッピングされて返る`() {
        every { userIdResolver.resolve(mockAccessToken) } returns "user-1"
        every { findMyBasketSummaryQueryService.summarize("user-1") } returns
            BasketSummaryView(totalQuantity = 5, totalTypeCount = 2, totalPrice = 470L)

        val response = withAuth { runBlocking { service.findMyBasketSummary(findMyBasketSummaryRequest {}) } }

        assertEquals(5, response.totalQuantity)
        assertEquals(2, response.totalTypeCount)
        assertEquals(470L, response.totalPrice)
    }

    @Test
    fun `カゴが空なら合計数量・種類数・合計金額はすべて 0 で返る`() {
        every { userIdResolver.resolve(mockAccessToken) } returns "user-2"
        every { findMyBasketSummaryQueryService.summarize("user-2") } returns
            BasketSummaryView(totalQuantity = 0, totalTypeCount = 0, totalPrice = 0L)

        val response = withAuth { runBlocking { service.findMyBasketSummary(findMyBasketSummaryRequest {}) } }

        assertEquals(0, response.totalQuantity)
        assertEquals(0, response.totalTypeCount)
        assertEquals(0L, response.totalPrice)
    }
}
