package jp.momiji.domain.product

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class ProductDescriptionTest {
    @Test
    fun `空文字なら Blank（商品説明は必須）`() {
        assertEquals(Err(ProductDescription.Blank), ProductDescription.create(""))
    }

    @Test
    fun `境界 2000 文字なら成功`() {
        val input = "a".repeat(ProductDescription.MAX_LENGTH)
        assertEquals(Ok(ProductDescription(input)), ProductDescription.create(input))
    }

    @Test
    fun `境界 + 1 (2001 文字) なら TooLong`() {
        val input = "a".repeat(ProductDescription.MAX_LENGTH + 1)
        assertEquals(Err(ProductDescription.TooLong), ProductDescription.create(input))
    }

    @Test
    fun `通常の説明文なら成功`() {
        assertEquals(Ok(ProductDescription("テスト説明")), ProductDescription.create("テスト説明"))
    }
}
