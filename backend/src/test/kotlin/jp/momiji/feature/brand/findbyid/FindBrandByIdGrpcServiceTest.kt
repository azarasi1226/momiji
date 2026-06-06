package jp.momiji.feature.brand.findbyid

import io.mockk.every
import io.mockk.mockk
import jp.momiji.domain.BusinessException
import jp.momiji.grpc.momiji.brand.findbyid.v1.findBrandByIdRequest
import jp.momiji.grpc.momiji.brand.v1.BrandStatus
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDateTime
import kotlin.test.assertEquals

class FindBrandByIdGrpcServiceTest {
    private val findBrandByIdQueryService = mockk<FindBrandByIdQueryService>()
    private val service = FindBrandByIdGrpcService(findBrandByIdQueryService)

    @Test
    fun `正常系_ブランドが存在すれば各フィールドを response に詰めて返す`() {
        every { findBrandByIdQueryService.findById("test-brand-id") } returns
            BrandView(
                id = "test-brand-id",
                name = "テストブランド",
                description = "テスト説明",
                status = "ACTIVE",
                createdAt = LocalDateTime.parse("2026-01-01T00:00:00"),
                updatedAt = LocalDateTime.parse("2026-01-02T00:00:00"),
            )

        val response =
            runBlocking {
                service.findBrandById(findBrandByIdRequest { id = "test-brand-id" })
            }

        assertEquals("test-brand-id", response.id)
        assertEquals("テストブランド", response.name)
        assertEquals("テスト説明", response.description)
        assertEquals(BrandStatus.BRAND_STATUS_ACTIVE, response.status)
    }

    @Test
    fun `異常系_ブランドが見つからなければ BusinessException`() {
        every { findBrandByIdQueryService.findById("missing-id") } returns null

        val ex =
            assertThrows<BusinessException> {
                runBlocking {
                    service.findBrandById(findBrandByIdRequest { id = "missing-id" })
                }
            }
        assertEquals("ブランドが見つかりません", ex.error.message)
    }
}
