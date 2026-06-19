package jp.momiji.feature.query.order.findmyorder

import io.grpc.Context
import io.mockk.every
import io.mockk.mockk
import jp.momiji.config.grpc.GrpcAuthContext
import jp.momiji.domain.BusinessException
import jp.momiji.feature.command.UserIdResolver
import jp.momiji.grpc.momiji.order.OrderStatus
import jp.momiji.grpc.momiji.order.findmyorder.findMyOrderRequest
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import java.time.LocalDateTime

class FindMyOrderGrpcServiceTest {
    private val userIdResolver = mockk<UserIdResolver>()
    private val findMyOrderQueryService = mockk<FindMyOrderQueryService>()
    private val service = FindMyOrderGrpcService(userIdResolver, findMyOrderQueryService)

    private val mockAccessToken = mockk<Jwt>()
    private val mockJwt = mockk<JwtAuthenticationToken> { every { token } returns mockAccessToken }

    private fun <T> withAuth(block: () -> T): T =
        Context
            .current()
            .withValue(GrpcAuthContext.AUTH_KEY, mockJwt)
            .call(block)

    @Test
    fun `本人の注文詳細が全情報（住所・カード・明細）込みで proto へマッピングされて返る`() {
        every { userIdResolver.resolve(mockAccessToken) } returns "user-1"
        every { findMyOrderQueryService.findByIdForUser("order-a", "user-1") } returns
            MyOrderDetailView(
                orderId = "order-a",
                status = "SHIPPED",
                totalAmount = 3000,
                createdAt = LocalDateTime.of(2026, 6, 18, 12, 0, 0),
                updatedAt = LocalDateTime.of(2026, 6, 19, 9, 0, 0),
                shippingAddress =
                    MyOrderDetailView.ShippingAddress(
                        recipientName = "受取 太郎",
                        phoneNumber = "090-1234-5678",
                        postalCode = "150-0041",
                        prefecture = "東京都",
                        city = "渋谷区",
                        streetAddress = "神南1-2-3",
                        building = "momijiビル 4F",
                        deliveryNote = "置き配可",
                    ),
                paymentMethod = MyOrderDetailView.PaymentMethod(id = "pm_1", brand = "visa", last4 = "4242"),
                items =
                    listOf(
                        MyOrderDetailView.Item(
                            productId = "prod-1",
                            name = "紅葉まんじゅう",
                            unitPrice = 1000,
                            quantity = 3,
                            imageUrl = "https://example.com/a.png",
                        ),
                    ),
            )

        val response = withAuth { runBlocking { service.findMyOrder(findMyOrderRequest { orderId = "order-a" }) } }

        assertEquals("order-a", response.orderId)
        assertEquals(OrderStatus.ORDER_STATUS_SHIPPED, response.status)
        assertEquals(3000, response.totalAmount)
        assertEquals("東京都", response.shippingAddress.prefecture)
        assertEquals("momijiビル 4F", response.shippingAddress.building)
        assertEquals("置き配可", response.shippingAddress.deliveryNote)
        assertTrue(response.hasPaymentMethod())
        assertEquals("visa", response.paymentMethod.brand)
        assertEquals("4242", response.paymentMethod.last4)
        assertEquals(1, response.itemsCount)
        assertEquals("紅葉まんじゅう", response.itemsList[0].name)
        assertEquals(1000, response.itemsList[0].unitPrice)
    }

    @Test
    fun `カード未設定なら payment_method は未設定で返る`() {
        every { userIdResolver.resolve(mockAccessToken) } returns "user-1"
        every { findMyOrderQueryService.findByIdForUser("order-b", "user-1") } returns
            MyOrderDetailView(
                orderId = "order-b",
                status = "STARTED",
                totalAmount = 500,
                createdAt = LocalDateTime.of(2026, 6, 19, 10, 0, 0),
                updatedAt = LocalDateTime.of(2026, 6, 19, 10, 0, 0),
                shippingAddress =
                    MyOrderDetailView.ShippingAddress(
                        recipientName = "受取 花子",
                        phoneNumber = "080-0000-0000",
                        postalCode = "220-0012",
                        prefecture = "神奈川県",
                        city = "横浜市西区",
                        streetAddress = "みなとみらい4-5-6",
                        building = "",
                        deliveryNote = "",
                    ),
                paymentMethod = null,
                items =
                    listOf(
                        MyOrderDetailView.Item("prod-2", "もみじ饅頭(こし)", 500, 1, null),
                    ),
            )

        val response = withAuth { runBlocking { service.findMyOrder(findMyOrderRequest { orderId = "order-b" }) } }

        assertFalse(response.hasPaymentMethod())
        // image_url 無しは空文字で返す。
        assertEquals("", response.itemsList[0].imageUrl)
    }

    @Test
    fun `本人の注文でない（不在含む）なら BusinessException`() {
        every { userIdResolver.resolve(mockAccessToken) } returns "user-2"
        every { findMyOrderQueryService.findByIdForUser("order-x", "user-2") } returns null

        assertThrows(BusinessException::class.java) {
            withAuth { runBlocking { service.findMyOrder(findMyOrderRequest { orderId = "order-x" }) } }
        }
    }
}
