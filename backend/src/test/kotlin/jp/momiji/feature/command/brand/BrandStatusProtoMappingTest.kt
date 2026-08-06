package jp.momiji.feature.command.brand

import jp.momiji.grpc.momiji.brand.BrandStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class BrandStatusProtoMappingTest {
    @Test
    fun `DB status 文字列を proto enum へ変換する`() {
        assertEquals(BrandStatus.BRAND_STATUS_ACTIVE, brandStatusToProto("ACTIVE"))
        assertEquals(BrandStatus.BRAND_STATUS_ARCHIVED, brandStatusToProto("ARCHIVED"))
    }

    @Test
    fun `未知の status 文字列は例外（stringly-typed を透過させない）`() {
        assertThrows(IllegalArgumentException::class.java) { brandStatusToProto("UNKNOWN") }
    }
}
