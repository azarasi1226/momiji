package jp.momiji.domain.stock

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class StockQuantityTest {
    @Test
    fun `負数なら OutOfRange`() {
        assertEquals(Err(StockQuantity.OutOfRange), StockQuantity.create(-1))
    }

    @Test
    fun `境界 0 なら成功`() {
        assertEquals(Ok(StockQuantity(StockQuantity.MIN)), StockQuantity.create(StockQuantity.MIN))
    }

    @Test
    fun `境界 上限なら成功`() {
        assertEquals(Ok(StockQuantity(StockQuantity.MAX)), StockQuantity.create(StockQuantity.MAX))
    }

    @Test
    fun `上限 + 1 なら OutOfRange`() {
        assertEquals(Err(StockQuantity.OutOfRange), StockQuantity.create(StockQuantity.MAX + 1))
    }
}
