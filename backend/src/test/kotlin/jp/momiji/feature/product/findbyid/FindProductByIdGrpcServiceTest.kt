package jp.momiji.feature.product.findbyid

import io.mockk.every
import io.mockk.mockk
import jp.momiji.domain.BusinessException
import jp.momiji.grpc.momiji.product.findbyid.v1.findProductByIdRequest
import jp.momiji.grpc.momiji.product.v1.ProductStatus
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FindProductByIdGrpcServiceTest {
    private val findProductByIdQueryService = mockk<FindProductByIdQueryService>()
    private val service = FindProductByIdGrpcService(findProductByIdQueryService)

    @Test
    fun `正常系_商品が存在すれば各フィールドを response に詰めて返す`() {
        every { findProductByIdQueryService.findById("test-product-id") } returns
            ProductView(
                id = "test-product-id",
                brandId = "test-brand-id",
                name = "テスト商品",
                description = "テスト説明",
                imageUrl = "https://example.com/i.png",
                price = 1000,
                status = "ACTIVE",
                createdAt = LocalDateTime.parse("2026-01-01T00:00:00"),
                updatedAt = LocalDateTime.parse("2026-01-02T00:00:00"),
            )

        val response =
            runBlocking {
                service.findProductById(findProductByIdRequest { id = "test-product-id" })
            }

        assertEquals("test-product-id", response.id)
        assertEquals("test-brand-id", response.brandId)
        assertEquals("テスト商品", response.name)
        assertEquals("テスト説明", response.description)
        assertTrue(response.hasImageUrl())
        assertEquals("https://example.com/i.png", response.imageUrl)
        assertEquals(1000, response.price)
        assertEquals(ProductStatus.PRODUCT_STATUS_ACTIVE, response.status)
    }

    @Test
    fun `正常系_imageUrlがnullなら response の image_url は未設定`() {
        every { findProductByIdQueryService.findById("test-product-id") } returns
            ProductView(
                id = "test-product-id",
                brandId = "test-brand-id",
                name = "テスト商品",
                description = "テスト説明",
                imageUrl = null,
                price = 1000,
                status = "DISCONTINUED",
                createdAt = LocalDateTime.parse("2026-01-01T00:00:00"),
                updatedAt = LocalDateTime.parse("2026-01-02T00:00:00"),
            )

        val response =
            runBlocking {
                service.findProductById(findProductByIdRequest { id = "test-product-id" })
            }

        assertFalse(response.hasImageUrl())
    }

    @Test
    fun `異常系_商品が見つからなければ BusinessException`() {
        every { findProductByIdQueryService.findById("missing-id") } returns null

        val ex =
            assertThrows<BusinessException> {
                runBlocking {
                    service.findProductById(findProductByIdRequest { id = "missing-id" })
                }
            }
        assertEquals("商品が見つかりません", ex.error.message)
    }
}
