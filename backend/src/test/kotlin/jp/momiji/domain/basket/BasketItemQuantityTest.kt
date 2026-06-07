package jp.momiji.domain.basket

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class BasketItemQuantityTest {
    @Test
    fun `下限 未満の 0 なら OutOfRange`() {
        assertEquals(Err(BasketItemQuantity.OutOfRange), BasketItemQuantity.create(0))
    }

    @Test
    fun `負数なら OutOfRange`() {
        assertEquals(Err(BasketItemQuantity.OutOfRange), BasketItemQuantity.create(-1))
    }

    @Test
    fun `境界 下限なら成功`() {
        assertEquals(Ok(BasketItemQuantity(BasketItemQuantity.MIN)), BasketItemQuantity.create(BasketItemQuantity.MIN))
    }

    @Test
    fun `境界 上限なら成功`() {
        assertEquals(Ok(BasketItemQuantity(BasketItemQuantity.MAX)), BasketItemQuantity.create(BasketItemQuantity.MAX))
    }

    @Test
    fun `上限 + 1 なら OutOfRange`() {
        assertEquals(Err(BasketItemQuantity.OutOfRange), BasketItemQuantity.create(BasketItemQuantity.MAX + 1))
    }
}
