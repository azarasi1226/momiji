package jp.momiji.domain.brand

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class BrandDescriptionTest {
    @Test
    fun `空文字は許可される`() {
        assertEquals(Ok(BrandDescription("")), BrandDescription.create(""))
    }

    @Test
    fun `境界 5000 文字なら成功`() {
        val input = "a".repeat(BrandDescription.MAX_LENGTH)
        assertEquals(Ok(BrandDescription(input)), BrandDescription.create(input))
    }

    @Test
    fun `境界 + 1 (5001 文字) なら TooLong`() {
        val input = "a".repeat(BrandDescription.MAX_LENGTH + 1)
        assertEquals(Err(BrandDescription.TooLong), BrandDescription.create(input))
    }

    @Test
    fun `通常の説明文なら成功`() {
        assertEquals(Ok(BrandDescription("テスト説明")), BrandDescription.create("テスト説明"))
    }
}
