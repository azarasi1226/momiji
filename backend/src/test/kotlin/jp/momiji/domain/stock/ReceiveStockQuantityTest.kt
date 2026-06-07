package jp.momiji.domain.stock

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class ReceiveStockQuantityTest {
    @Test
    fun `0 なら OutOfRange`() {
        assertEquals(Err(ReceiveStockQuantity.OutOfRange), ReceiveStockQuantity.create(0))
    }

    @Test
    fun `負数なら OutOfRange`() {
        assertEquals(Err(ReceiveStockQuantity.OutOfRange), ReceiveStockQuantity.create(-1))
    }

    @Test
    fun `境界 1 なら成功`() {
        assertEquals(
            Ok(ReceiveStockQuantity(ReceiveStockQuantity.MIN)),
            ReceiveStockQuantity.create(ReceiveStockQuantity.MIN),
        )
    }

    @Test
    fun `境界 上限なら成功`() {
        assertEquals(
            Ok(ReceiveStockQuantity(ReceiveStockQuantity.MAX)),
            ReceiveStockQuantity.create(ReceiveStockQuantity.MAX),
        )
    }

    @Test
    fun `上限 + 1 なら OutOfRange`() {
        assertEquals(
            Err(ReceiveStockQuantity.OutOfRange),
            ReceiveStockQuantity.create(ReceiveStockQuantity.MAX + 1),
        )
    }
}
