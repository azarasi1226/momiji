package jp.momiji.feature.command.stock

import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.get
import com.github.michaelbull.result.getError
import jp.momiji.domain.stock.StockAdjustmentReason
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import jp.momiji.grpc.momiji.stock.StockAdjustmentReason as ProtoStockAdjustmentReason

class StockAdjustmentReasonProtoMappingTest {
    @Test
    fun `proto の各理由をドメイン enum へ変換する`() {
        val cases =
            listOf(
                ProtoStockAdjustmentReason.STOCK_ADJUSTMENT_REASON_DAMAGED to StockAdjustmentReason.DAMAGED,
                ProtoStockAdjustmentReason.STOCK_ADJUSTMENT_REASON_LOST to StockAdjustmentReason.LOST,
                ProtoStockAdjustmentReason.STOCK_ADJUSTMENT_REASON_STOCKTAKING to StockAdjustmentReason.STOCKTAKING,
                ProtoStockAdjustmentReason.STOCK_ADJUSTMENT_REASON_CORRECTION to StockAdjustmentReason.CORRECTION,
                ProtoStockAdjustmentReason.STOCK_ADJUSTMENT_REASON_OTHER to StockAdjustmentReason.OTHER,
            )

        cases.forEach { (proto, expected) ->
            assertEquals(Ok(expected), stockAdjustmentReasonFromProto(proto))
        }
    }

    @Test
    fun `UNSPECIFIED は reason の ValidationError`() {
        val result = stockAdjustmentReasonFromProto(ProtoStockAdjustmentReason.STOCK_ADJUSTMENT_REASON_UNSPECIFIED)

        assertNull(result.get())
        assertEquals("reason", result.getError()?.field)
    }

    @Test
    fun `未知の値（UNRECOGNIZED）も reason の ValidationError`() {
        val result = stockAdjustmentReasonFromProto(ProtoStockAdjustmentReason.UNRECOGNIZED)

        assertNull(result.get())
        assertEquals("reason", result.getError()?.field)
    }
}
