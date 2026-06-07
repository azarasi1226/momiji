package jp.momiji.domain.stock

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class AdjustStockQuantityTest {
    @Test
    fun `0 なら Zero`() {
        assertEquals(Err(AdjustStockQuantity.Zero), AdjustStockQuantity.create(0))
    }

    @Test
    fun `正の値なら成功`() {
        assertEquals(Ok(AdjustStockQuantity(5)), AdjustStockQuantity.create(5))
    }

    @Test
    fun `負の値なら成功`() {
        assertEquals(Ok(AdjustStockQuantity(-3)), AdjustStockQuantity.create(-3))
    }

    @Test
    fun `大きさが上限超過なら OutOfRange`() {
        assertEquals(
            Err(AdjustStockQuantity.OutOfRange),
            AdjustStockQuantity.create(AdjustStockQuantity.MAX_MAGNITUDE + 1),
        )
    }

    @Test
    fun `負方向に大きさ超過でも OutOfRange`() {
        assertEquals(
            Err(AdjustStockQuantity.OutOfRange),
            AdjustStockQuantity.create(-(AdjustStockQuantity.MAX_MAGNITUDE + 1)),
        )
    }
}
