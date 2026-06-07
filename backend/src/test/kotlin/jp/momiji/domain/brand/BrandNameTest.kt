package jp.momiji.domain.brand

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class BrandNameTest {
    @Test
    fun `空文字なら Blank`() {
        assertEquals(Err(BrandName.Blank), BrandName.create(""))
    }

    @Test
    fun `空白のみなら Blank`() {
        assertEquals(Err(BrandName.Blank), BrandName.create("   "))
    }

    @Test
    fun `境界 100 文字なら成功`() {
        val input = "a".repeat(BrandName.MAX_LENGTH)
        assertEquals(Ok(BrandName(input)), BrandName.create(input))
    }

    @Test
    fun `境界 + 1 (101 文字) なら TooLong`() {
        val input = "a".repeat(BrandName.MAX_LENGTH + 1)
        assertEquals(Err(BrandName.TooLong), BrandName.create(input))
    }

    @Test
    fun `通常のブランド名なら成功`() {
        assertEquals(Ok(BrandName("テストブランド")), BrandName.create("テストブランド"))
    }
}
