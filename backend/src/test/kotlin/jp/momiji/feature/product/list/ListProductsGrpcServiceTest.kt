package jp.momiji.feature.product.list

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import jp.momiji.feature.query.Page
import jp.momiji.feature.query.Paging
import jp.momiji.feature.query.product.findbyid.ProductView
import jp.momiji.feature.query.product.list.ListProductsGrpcService
import jp.momiji.feature.query.product.list.ListProductsQuery
import jp.momiji.feature.query.product.list.ListProductsQueryService
import jp.momiji.feature.query.product.list.ProductSort
import jp.momiji.grpc.momiji.product.list.v1.listProductsRequest
import jp.momiji.grpc.momiji.product.v1.ProductSortCondition
import jp.momiji.grpc.momiji.product.v1.ProductStatus
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import kotlin.test.assertEquals
import jp.momiji.domain.product.ProductStatus as DomainProductStatus

class ListProductsGrpcServiceTest {
    private val listProductsQueryService = mockk<ListProductsQueryService>()
    private val service = ListProductsGrpcService(listProductsQueryService)

    private fun view(
        id: String,
        name: String,
    ) = ProductView(
        id = id,
        brandId = "brand-a",
        name = name,
        description = "説明",
        imageUrl = null,
        price = 1000,
        status = "ACTIVE",
        createdAt = LocalDateTime.parse("2026-01-01T00:00:00"),
        updatedAt = LocalDateTime.parse("2026-01-02T00:00:00"),
    )

    @Test
    fun `正常系_ページとpagingメタを response に詰めて返す`() {
        every { listProductsQueryService.list(any()) } returns
            Page(
                items = listOf(view("01ARZ3NDEKTSV4RRFFQ69G5FAV", "A商品")),
                paging = Paging(totalCount = 45, pageSize = 20, pageNumber = 2),
            )

        val response =
            runBlocking {
                service.listProducts(
                    listProductsRequest {
                        pageSize = 20
                        pageNumber = 2
                    },
                )
            }

        assertEquals(1, response.productsList.size)
        assertEquals("A商品", response.productsList[0].name)
        assertEquals(ProductStatus.PRODUCT_STATUS_ACTIVE, response.productsList[0].status)
        assertEquals(45, response.paging.totalCount)
        assertEquals(3, response.paging.totalPage) // ceil(45/20)
        assertEquals(20, response.paging.pageSize)
        assertEquals(2, response.paging.pageNumber)
    }

    @Test
    fun `正常系_検索語・ソート・ページが query に正しく渡る`() {
        val querySlot = slot<ListProductsQuery>()
        every { listProductsQueryService.list(capture(querySlot)) } returns
            Page(items = emptyList(), paging = Paging(totalCount = 0, pageSize = 10, pageNumber = 3))

        runBlocking {
            service.listProducts(
                listProductsRequest {
                    likeName = "シャツ"
                    sort = ProductSortCondition.PRODUCT_SORT_CONDITION_PRICE_DESC
                    status = ProductStatus.PRODUCT_STATUS_DISCONTINUED
                    brandId = "01HXYZBRAND0000000000000B1"
                    pageSize = 10
                    pageNumber = 3
                },
            )
        }

        val query = querySlot.captured
        assertEquals("シャツ", query.likeName)
        assertEquals(ProductSort.PRICE_DESC, query.sort)
        assertEquals(DomainProductStatus.DISCONTINUED, query.status)
        assertEquals("01HXYZBRAND0000000000000B1", query.brandId)
        assertEquals(10, query.paging.pageSize)
        assertEquals(3, query.paging.pageNumber)
    }

    @Test
    fun `既定値_pageSize0_pageNumber0_sort未指定は既定に丸める`() {
        val querySlot = slot<ListProductsQuery>()
        every { listProductsQueryService.list(capture(querySlot)) } returns
            Page(items = emptyList(), paging = Paging(totalCount = 0, pageSize = 20, pageNumber = 1))

        runBlocking {
            service.listProducts(listProductsRequest { })
        }

        val query = querySlot.captured
        assertEquals(20, query.paging.pageSize) // DEFAULT_PAGE_SIZE
        assertEquals(1, query.paging.pageNumber) // 1 始まり
        assertEquals(ProductSort.NAME_ASC, query.sort) // UNSPECIFIED → 既定
        assertEquals(null, query.status) // 状態 UNSPECIFIED → フィルタなし
        assertEquals("", query.brandId) // brand_id 空 → フィルタなし（query 側で isBlank 判定）
        verify(exactly = 1) { listProductsQueryService.list(any()) }
    }
}
