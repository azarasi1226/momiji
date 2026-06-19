package jp.momiji.feature.query.order.listmyorders

import io.grpc.Context
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import jp.momiji.config.grpc.GrpcAuthContext
import jp.momiji.feature.command.UserIdResolver
import jp.momiji.feature.query.Page
import jp.momiji.feature.query.Paging
import jp.momiji.feature.query.PagingCondition
import jp.momiji.grpc.momiji.order.OrderStatus
import jp.momiji.grpc.momiji.order.listmyorders.listMyOrdersRequest
import jp.momiji.util.toProtoTimestamp
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import java.time.LocalDateTime

class ListMyOrdersGrpcServiceTest {
    private val userIdResolver = mockk<UserIdResolver>()
    private val listMyOrdersQueryService = mockk<ListMyOrdersQueryService>()
    private val service = ListMyOrdersGrpcService(userIdResolver, listMyOrdersQueryService)

    private val mockAccessToken = mockk<Jwt>()
    private val mockJwt = mockk<JwtAuthenticationToken> { every { token } returns mockAccessToken }

    private fun <T> withAuth(block: () -> T): T =
        Context
            .current()
            .withValue(GrpcAuthContext.AUTH_KEY, mockJwt)
            .call(block)

    @Test
    fun `JWT解決したuserIdの注文一覧がproto（status・明細・paging）へマッピングされて返る`() {
        every { userIdResolver.resolve(mockAccessToken) } returns "user-1"
        every { listMyOrdersQueryService.findByUserId("user-1", any(), any(), any()) } returns
            Page(
                items =
                    listOf(
                        MyOrderView(
                            orderId = "order-a",
                            status = "SHIPPED",
                            totalAmount = 3000,
                            createdAt = LocalDateTime.of(2026, 6, 18, 12, 0, 0),
                            items =
                                listOf(
                                    MyOrderView.Item(
                                        productId = "prod-1",
                                        name = "紅葉まんじゅう",
                                        unitPrice = 1000,
                                        quantity = 3,
                                        imageUrl = "https://example.com/a.png",
                                    ),
                                ),
                        ),
                    ),
                paging = Paging(totalCount = 1, pageSize = 20, pageNumber = 1),
            )

        val response = withAuth { runBlocking { service.listMyOrders(listMyOrdersRequest {}) } }

        assertEquals(1, response.ordersCount)
        val order = response.ordersList[0]
        assertEquals("order-a", order.orderId)
        assertEquals(OrderStatus.ORDER_STATUS_SHIPPED, order.status)
        assertEquals(3000, order.totalAmount)
        assertEquals(1, order.itemsCount)
        val item = order.itemsList[0]
        assertEquals("prod-1", item.productId)
        assertEquals("紅葉まんじゅう", item.name)
        assertEquals(1000, item.unitPrice)
        assertEquals(3, item.quantity)
        assertEquals("https://example.com/a.png", item.imageUrl)
        assertEquals(1, response.paging.totalCount)
        assertEquals(1, response.paging.totalPage)
        assertEquals(20, response.paging.pageSize)
        assertEquals(1, response.paging.pageNumber)
    }

    @Test
    fun `リクエストの pageSize・pageNumber が正規化されて query へ渡る`() {
        every { userIdResolver.resolve(mockAccessToken) } returns "user-2"
        val pagingSlot = slot<PagingCondition>()
        every { listMyOrdersQueryService.findByUserId("user-2", capture(pagingSlot), any(), any()) } returns
            Page(items = emptyList(), paging = Paging(totalCount = 0, pageSize = 20, pageNumber = 1))

        val response =
            withAuth {
                runBlocking {
                    service.listMyOrders(
                        listMyOrdersRequest {
                            pageSize = 0 // 0 → 既定（20）へ正規化される
                            pageNumber = 0 // 0 → 1 へ正規化される
                        },
                    )
                }
            }

        assertEquals(0, response.ordersCount)
        assertEquals(PagingCondition.DEFAULT_PAGE_SIZE, pagingSlot.captured.pageSize)
        assertEquals(1, pagingSlot.captured.pageNumber)
    }

    @Test
    fun `期間（created_from・created_to）が UTC LocalDateTime に変換されて query へ渡る`() {
        every { userIdResolver.resolve(mockAccessToken) } returns "user-3"
        val fromList = mutableListOf<LocalDateTime?>()
        val toList = mutableListOf<LocalDateTime?>()
        every {
            listMyOrdersQueryService.findByUserId("user-3", any(), captureNullable(fromList), captureNullable(toList))
        } returns Page(items = emptyList(), paging = Paging(totalCount = 0, pageSize = 20, pageNumber = 1))

        // 「2025年」を JST で区切った半開区間 [2025-01-01, 2026-01-01) を UTC 絶対時刻にしたもの（BFF 相当）。
        val from = LocalDateTime.of(2024, 12, 31, 15, 0, 0) // 2025-01-01T00:00+09:00
        val to = LocalDateTime.of(2025, 12, 31, 15, 0, 0) // 2026-01-01T00:00+09:00

        withAuth {
            runBlocking {
                service.listMyOrders(
                    listMyOrdersRequest {
                        createdFrom = from.toProtoTimestamp()
                        createdTo = to.toProtoTimestamp()
                    },
                )
            }
        }

        assertEquals(from, fromList.last())
        assertEquals(to, toList.last())
    }

    @Test
    fun `期間未指定なら created_from・created_to は null で渡る`() {
        every { userIdResolver.resolve(mockAccessToken) } returns "user-4"
        val fromList = mutableListOf<LocalDateTime?>()
        val toList = mutableListOf<LocalDateTime?>()
        every {
            listMyOrdersQueryService.findByUserId("user-4", any(), captureNullable(fromList), captureNullable(toList))
        } returns Page(items = emptyList(), paging = Paging(totalCount = 0, pageSize = 20, pageNumber = 1))

        withAuth { runBlocking { service.listMyOrders(listMyOrdersRequest {}) } }

        assertEquals(null, fromList.last())
        assertEquals(null, toList.last())
    }
}
