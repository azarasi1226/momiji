package jp.momiji.feature.product.list

import io.mockk.every
import io.mockk.mockk
import jp.momiji.feature.product.findbyid.ProductView
import jp.momiji.grpc.momiji.product.list.v1.listProductsRequest
import jp.momiji.grpc.momiji.product.v1.ProductStatus
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ListProductsGrpcServiceTest {
    private val listProductsQueryService = mockk<ListProductsQueryService>()
    private val service = ListProductsGrpcService(listProductsQueryService)

    @Test
    fun `正常系_クエリ結果を response に詰めて返す`() {
        every { listProductsQueryService.findAll() } returns
            listOf(
                ProductView(
                    id = "01ARZ3NDEKTSV4RRFFQ69G5FAV",
                    brandId = "brand-a",
                    name = "A商品",
                    description = "説明A",
                    imageUrl = "https://example.com/a.png",
                    price = 1000,
                    status = "ACTIVE",
                    createdAt = LocalDateTime.parse("2026-01-01T00:00:00"),
                    updatedAt = LocalDateTime.parse("2026-01-02T00:00:00"),
                ),
                ProductView(
                    id = "01BX5ZZKBKACTAV9WEVGEMMVRZ",
                    brandId = "brand-b",
                    name = "B商品",
                    description = "説明B",
                    imageUrl = null,
                    price = 2000,
                    status = "DISCONTINUED",
                    createdAt = LocalDateTime.parse("2026-02-01T00:00:00"),
                    updatedAt = LocalDateTime.parse("2026-02-02T00:00:00"),
                ),
            )

        val response =
            runBlocking {
                service.listProducts(listProductsRequest { })
            }

        assertEquals(2, response.productsList.size)
        assertEquals("01ARZ3NDEKTSV4RRFFQ69G5FAV", response.productsList[0].id)
        assertEquals("A商品", response.productsList[0].name)
        assertTrue(response.productsList[0].hasImageUrl())
        assertEquals(ProductStatus.PRODUCT_STATUS_ACTIVE, response.productsList[0].status)
        assertEquals("B商品", response.productsList[1].name)
        assertFalse(response.productsList[1].hasImageUrl())
        assertEquals(ProductStatus.PRODUCT_STATUS_DISCONTINUED, response.productsList[1].status)
    }

    @Test
    fun `正常系_0件なら空リスト`() {
        every { listProductsQueryService.findAll() } returns emptyList()

        val response =
            runBlocking {
                service.listProducts(listProductsRequest { })
            }

        assertEquals(0, response.productsList.size)
    }
}
