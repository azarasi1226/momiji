package jp.momiji.domain.user

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class Address2Test {
    @Test
    fun `空文字でも成功 (任意項目)`() {
        assertEquals(Ok(Address2("")), Address2.create(""))
    }

    @Test
    fun `境界 200 文字なら成功`() {
        val input = "あ".repeat(Address2.MAX_LENGTH)
        assertEquals(Ok(Address2(input)), Address2.create(input))
    }

    @Test
    fun `境界 + 1 なら TooLong`() {
        val input = "あ".repeat(Address2.MAX_LENGTH + 1)
        assertEquals(Err(Address2.TooLong), Address2.create(input))
    }

    @Test
    fun `通常の補助行なら成功`() {
        assertEquals(Ok(Address2("ビル 5F")), Address2.create("ビル 5F"))
    }
}
