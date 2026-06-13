package jp.momiji.feature.query.user.shippingaddress.list

import io.grpc.Context
import io.mockk.every
import io.mockk.mockk
import jp.momiji.config.grpc.GrpcAuthContext
import jp.momiji.feature.command.UserIdResolver
import jp.momiji.grpc.momiji.user.shippingaddress.list.v1.listShippingAddressesRequest
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken

class ListShippingAddressesGrpcServiceTest {
    private val userIdResolver = mockk<UserIdResolver>()
    private val listShippingAddressesQueryService = mockk<ListShippingAddressesQueryService>()
    private val service = ListShippingAddressesGrpcService(userIdResolver, listShippingAddressesQueryService)

    private val mockAccessToken = mockk<Jwt>()
    private val mockJwt = mockk<JwtAuthenticationToken> { every { token } returns mockAccessToken }

    private fun <T> withAuth(block: () -> T): T =
        Context
            .current()
            .withValue(GrpcAuthContext.AUTH_KEY, mockJwt)
            .call(block)

    @Test
    fun `JWT解決したuserIdの配送先一覧がprotoへマッピングされて返る`() {
        every { userIdResolver.resolve(mockAccessToken) } returns "user-1"
        every { listShippingAddressesQueryService.findByUserId("user-1") } returns
            listOf(
                ShippingAddressView(
                    id = "addr-a",
                    name = "受取 太郎",
                    phoneNumber = "090-1234-5678",
                    postalCode = "150-0041",
                    prefecture = "東京都",
                    city = "渋谷区",
                    streetAddress = "神南1-2-3",
                    building = "momijiビル 4F",
                    deliveryNote = "置き配可",
                    isDefault = true,
                ),
                ShippingAddressView(
                    id = "addr-b",
                    name = "受取 花子",
                    phoneNumber = "080-9999-0000",
                    postalCode = "220-0012",
                    prefecture = "神奈川県",
                    city = "横浜市西区",
                    streetAddress = "みなとみらい4-5-6",
                    building = "",
                    deliveryNote = "",
                    isDefault = false,
                ),
            )

        val response = withAuth { runBlocking { service.listShippingAddresses(listShippingAddressesRequest {}) } }

        assertEquals(2, response.shippingAddressesCount)
        val first = response.shippingAddressesList[0]
        assertEquals("addr-a", first.id)
        assertEquals("受取 太郎", first.name)
        assertEquals("090-1234-5678", first.phoneNumber)
        assertEquals("150-0041", first.postalCode)
        assertEquals("東京都", first.prefecture)
        assertEquals("渋谷区", first.city)
        assertEquals("神南1-2-3", first.streetAddress)
        assertEquals("momijiビル 4F", first.building)
        assertEquals("置き配可", first.deliveryNote)
        assertEquals(true, first.isDefault)
        val second = response.shippingAddressesList[1]
        assertEquals("addr-b", second.id)
        assertEquals(false, second.isDefault)
    }

    @Test
    fun `配送先が無ければ空の一覧が返る`() {
        every { userIdResolver.resolve(mockAccessToken) } returns "user-2"
        every { listShippingAddressesQueryService.findByUserId("user-2") } returns emptyList()

        val response = withAuth { runBlocking { service.listShippingAddresses(listShippingAddressesRequest {}) } }

        assertEquals(0, response.shippingAddressesCount)
    }
}
