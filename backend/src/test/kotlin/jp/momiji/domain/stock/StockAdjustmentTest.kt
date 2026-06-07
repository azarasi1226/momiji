package jp.momiji.domain.stock

import com.github.michaelbull.result.get
import com.github.michaelbull.result.getError
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class StockAdjustmentTest {
    private fun quantity(value: Int) = AdjustStockQuantity.create(value).get()!!

    @Test
    fun `棚卸しなら増加できる`() {
        val result = StockAdjustment.create(quantity(5), StockAdjustmentReason.STOCKTAKING)
        assertNotNull(result.get())
    }

    @Test
    fun `棚卸し以外で増加は IncreaseNotAllowed`() {
        for (reason in listOf(
            StockAdjustmentReason.DAMAGED,
            StockAdjustmentReason.LOST,
            StockAdjustmentReason.CORRECTION,
            StockAdjustmentReason.OTHER,
        )) {
            val result = StockAdjustment.create(quantity(5), reason)
            assertEquals(StockAdjustment.IncreaseNotAllowed, result.getError(), "reason=$reason")
        }
    }

    @Test
    fun `減少はどの理由でも可能`() {
        for (reason in StockAdjustmentReason.entries) {
            val result = StockAdjustment.create(quantity(-1), reason)
            assertNull(result.getError(), "reason=$reason")
        }
    }
}
