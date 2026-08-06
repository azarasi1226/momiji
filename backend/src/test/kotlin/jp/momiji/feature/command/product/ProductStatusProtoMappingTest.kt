package jp.momiji.feature.command.product

import jp.momiji.domain.product.ProductStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import jp.momiji.grpc.momiji.product.ProductStatus as ProtoProductStatus

class ProductStatusProtoMappingTest {
    @Test
    fun `DB status 文字列を proto enum へ変換する`() {
        assertEquals(ProtoProductStatus.PRODUCT_STATUS_ACTIVE, productStatusToProto("ACTIVE"))
        assertEquals(ProtoProductStatus.PRODUCT_STATUS_DISCONTINUED, productStatusToProto("DISCONTINUED"))
    }

    @Test
    fun `未知の status 文字列は例外`() {
        assertThrows(IllegalArgumentException::class.java) { productStatusToProto("UNKNOWN") }
    }

    @Test
    fun `proto enum を状態フィルタのドメイン enum へ変換する`() {
        assertEquals(ProductStatus.ACTIVE, productStatusFilterFromProto(ProtoProductStatus.PRODUCT_STATUS_ACTIVE))
        assertEquals(
            ProductStatus.DISCONTINUED,
            productStatusFilterFromProto(ProtoProductStatus.PRODUCT_STATUS_DISCONTINUED),
        )
    }

    @Test
    fun `UNSPECIFIED・未知の値はフィルタなし（null）`() {
        assertNull(productStatusFilterFromProto(ProtoProductStatus.PRODUCT_STATUS_UNSPECIFIED))
        assertNull(productStatusFilterFromProto(ProtoProductStatus.UNRECOGNIZED))
    }
}
