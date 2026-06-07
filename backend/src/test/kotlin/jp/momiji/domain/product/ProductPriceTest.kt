package jp.momiji.domain.product

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class ProductPriceTest {
    @Test
    fun `下限 0 なら OutOfRange`() {
        assertEquals(Err(ProductPrice.OutOfRange), ProductPrice.create(0))
    }

    @Test
    fun `負数なら OutOfRange`() {
        assertEquals(Err(ProductPrice.OutOfRange), ProductPrice.create(-1))
    }

    @Test
    fun `境界 1 なら成功`() {
        assertEquals(Ok(ProductPrice(ProductPrice.MIN)), ProductPrice.create(ProductPrice.MIN))
    }

    @Test
    fun `境界 上限なら成功`() {
        assertEquals(Ok(ProductPrice(ProductPrice.MAX)), ProductPrice.create(ProductPrice.MAX))
    }

    @Test
    fun `上限 + 1 なら OutOfRange`() {
        assertEquals(Err(ProductPrice.OutOfRange), ProductPrice.create(ProductPrice.MAX + 1))
    }
}
