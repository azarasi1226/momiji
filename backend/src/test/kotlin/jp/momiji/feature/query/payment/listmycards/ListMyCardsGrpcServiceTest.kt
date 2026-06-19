package jp.momiji.feature.query.payment.listmycards

import io.grpc.Context
import io.mockk.every
import io.mockk.mockk
import jp.momiji.config.grpc.GrpcAuthContext
import jp.momiji.feature.command.UserIdResolver
import jp.momiji.grpc.momiji.payment.listmycards.listMyCardsRequest
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken

class ListMyCardsGrpcServiceTest {
    private val userIdResolver = mockk<UserIdResolver>()
    private val listMyCardsQueryService = mockk<ListMyCardsQueryService>()
    private val service = ListMyCardsGrpcService(userIdResolver, listMyCardsQueryService)

    private val mockAccessToken = mockk<Jwt>()
    private val mockJwt = mockk<JwtAuthenticationToken> { every { token } returns mockAccessToken }

    private fun <T> withAuth(block: () -> T): T =
        Context
            .current()
            .withValue(GrpcAuthContext.AUTH_KEY, mockJwt)
            .call(block)

    @Test
    fun `JWT解決したuserIdのカード一覧がprotoへマッピングされて返る`() {
        every { userIdResolver.resolve(mockAccessToken) } returns "user-1"
        every { listMyCardsQueryService.findByUserId("user-1") } returns
            listOf(
                CardView(id = "pm_a", brand = "visa", last4 = "4242", expMonth = 2, expYear = 2032, isDefault = true),
                CardView(id = "pm_b", brand = "mastercard", last4 = "4444", expMonth = 12, expYear = 2030, isDefault = false),
            )

        val response = withAuth { runBlocking { service.listMyCards(listMyCardsRequest {}) } }

        assertEquals(2, response.cardsCount)
        val first = response.cardsList[0]
        assertEquals("pm_a", first.id)
        assertEquals("visa", first.brand)
        assertEquals("4242", first.last4)
        assertEquals(2, first.expMonth)
        assertEquals(2032, first.expYear)
        assertEquals(true, first.isDefault)
        assertEquals("pm_b", response.cardsList[1].id)
    }

    @Test
    fun `カードが無ければ空の一覧が返る`() {
        every { userIdResolver.resolve(mockAccessToken) } returns "user-2"
        every { listMyCardsQueryService.findByUserId("user-2") } returns emptyList()

        val response = withAuth { runBlocking { service.listMyCards(listMyCardsRequest {}) } }

        assertEquals(0, response.cardsCount)
    }
}
