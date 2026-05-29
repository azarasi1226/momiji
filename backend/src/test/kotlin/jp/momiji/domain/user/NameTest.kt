package jp.momiji.domain.user

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class NameTest {
    @Test
    fun `空文字なら Blank`() {
        assertEquals(Err(Name.Blank), Name.create(""))
    }

    @Test
    fun `空白のみなら Blank`() {
        assertEquals(Err(Name.Blank), Name.create("   "))
    }

    @Test
    fun `境界 100 文字なら成功`() {
        val input = "a".repeat(Name.MAX_LENGTH)
        assertEquals(Ok(Name(input)), Name.create(input))
    }

    @Test
    fun `境界 + 1 (101 文字) なら TooLong`() {
        val input = "a".repeat(Name.MAX_LENGTH + 1)
        assertEquals(Err(Name.TooLong), Name.create(input))
    }

    @Test
    fun `通常の名前なら成功`() {
        assertEquals(Ok(Name("Alice")), Name.create("Alice"))
    }
}
