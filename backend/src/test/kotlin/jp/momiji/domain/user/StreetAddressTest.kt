package jp.momiji.domain.user

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class StreetAddressTest {
    @Test
    fun `空文字なら Blank`() {
        assertEquals(Err(StreetAddress.Blank), StreetAddress.create(""))
    }

    @Test
    fun `空白のみなら Blank`() {
        assertEquals(Err(StreetAddress.Blank), StreetAddress.create("   "))
    }

    @Test
    fun `上限超過なら TooLong`() {
        assertEquals(Err(StreetAddress.TooLong), StreetAddress.create("a".repeat(StreetAddress.MAX_LENGTH + 1)))
    }

    @Test
    fun `上限ちょうど（MAX_LENGTH 文字）なら成功`() {
        val input = "a".repeat(StreetAddress.MAX_LENGTH)
        assertEquals(Ok(StreetAddress(input)), StreetAddress.create(input))
    }

    @Test
    fun `通常の番地なら成功`() {
        assertEquals(Ok(StreetAddress("1-2-3")), StreetAddress.create("1-2-3"))
    }
}
