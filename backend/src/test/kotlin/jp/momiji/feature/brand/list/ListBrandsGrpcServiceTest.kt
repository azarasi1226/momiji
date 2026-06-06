package jp.momiji.feature.brand.list

import io.mockk.every
import io.mockk.mockk
import jp.momiji.feature.brand.findbyid.BrandView
import jp.momiji.grpc.momiji.brand.list.v1.listBrandsRequest
import jp.momiji.grpc.momiji.brand.v1.BrandStatus
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import kotlin.test.assertEquals

class ListBrandsGrpcServiceTest {
    private val listBrandsQueryService = mockk<ListBrandsQueryService>()
    private val service = ListBrandsGrpcService(listBrandsQueryService)

    @Test
    fun `正常系_クエリ結果を response に詰めて返す`() {
        every { listBrandsQueryService.findAll() } returns
            listOf(
                BrandView(
                    id = "01ARZ3NDEKTSV4RRFFQ69G5FAV",
                    name = "Aブランド",
                    description = "説明A",
                    status = "ACTIVE",
                    createdAt = LocalDateTime.parse("2026-01-01T00:00:00"),
                    updatedAt = LocalDateTime.parse("2026-01-02T00:00:00"),
                ),
                BrandView(
                    id = "01BX5ZZKBKACTAV9WEVGEMMVRZ",
                    name = "Bブランド",
                    description = "",
                    status = "ARCHIVED",
                    createdAt = LocalDateTime.parse("2026-02-01T00:00:00"),
                    updatedAt = LocalDateTime.parse("2026-02-02T00:00:00"),
                ),
            )

        val response =
            runBlocking {
                service.listBrands(listBrandsRequest { })
            }

        assertEquals(2, response.brandsList.size)
        assertEquals("01ARZ3NDEKTSV4RRFFQ69G5FAV", response.brandsList[0].id)
        assertEquals("Aブランド", response.brandsList[0].name)
        assertEquals("説明A", response.brandsList[0].description)
        assertEquals(BrandStatus.BRAND_STATUS_ACTIVE, response.brandsList[0].status)
        assertEquals("Bブランド", response.brandsList[1].name)
        assertEquals(BrandStatus.BRAND_STATUS_ARCHIVED, response.brandsList[1].status)
    }

    @Test
    fun `正常系_0件なら空リスト`() {
        every { listBrandsQueryService.findAll() } returns emptyList()

        val response =
            runBlocking {
                service.listBrands(listBrandsRequest { })
            }

        assertEquals(0, response.brandsList.size)
    }
}
