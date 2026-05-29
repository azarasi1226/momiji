package jp.momiji.domain.user

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class Address1Test {
    @Test
    fun `空文字なら Blank`() {
        assertEquals(Err(Address1.Blank), Address1.create(""))
    }

    @Test
    fun `空白のみなら Blank`() {
        assertEquals(Err(Address1.Blank), Address1.create("   "))
    }

    @Test
    fun `境界 200 文字なら成功`() {
        val input = "あ".repeat(Address1.MAX_LENGTH)
        assertEquals(Ok(Address1(input)), Address1.create(input))
    }

    @Test
    fun `境界 + 1 なら TooLong`() {
        val input = "あ".repeat(Address1.MAX_LENGTH + 1)
        assertEquals(Err(Address1.TooLong), Address1.create(input))
    }

    @Test
    fun `通常の住所なら成功`() {
        assertEquals(Ok(Address1("東京都千代田区千代田 1-1")), Address1.create("東京都千代田区千代田 1-1"))
    }
}
