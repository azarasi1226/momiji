package jp.momiji.domain.product

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class ProductNameTest {
    @Test
    fun `空文字なら Blank`() {
        assertEquals(Err(ProductName.Blank), ProductName.create(""))
    }

    @Test
    fun `空白のみなら Blank`() {
        assertEquals(Err(ProductName.Blank), ProductName.create("   "))
    }

    @Test
    fun `境界 200 文字なら成功`() {
        val input = "a".repeat(ProductName.MAX_LENGTH)
        assertEquals(Ok(ProductName(input)), ProductName.create(input))
    }

    @Test
    fun `境界 + 1 (201 文字) なら TooLong`() {
        val input = "a".repeat(ProductName.MAX_LENGTH + 1)
        assertEquals(Err(ProductName.TooLong), ProductName.create(input))
    }

    @Test
    fun `通常の商品名なら成功`() {
        assertEquals(Ok(ProductName("テスト商品")), ProductName.create("テスト商品"))
    }
}
