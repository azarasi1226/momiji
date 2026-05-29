package jp.momiji.domain.user

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class EmailChangeTokenTest {
    @Test
    fun `空文字なら Blank`() {
        assertEquals(Err(EmailChangeToken.Blank), EmailChangeToken.create(""))
    }

    @Test
    fun `空白のみなら Blank`() {
        assertEquals(Err(EmailChangeToken.Blank), EmailChangeToken.create("   "))
    }

    @Test
    fun `ドット 1 つだけなら InvalidFormat`() {
        assertEquals(Err(EmailChangeToken.InvalidFormat), EmailChangeToken.create("header.payload"))
    }

    @Test
    fun `ドット 3 つなら InvalidFormat`() {
        assertEquals(Err(EmailChangeToken.InvalidFormat), EmailChangeToken.create("a.b.c.d"))
    }

    @Test
    fun `ドット無しなら InvalidFormat`() {
        assertEquals(Err(EmailChangeToken.InvalidFormat), EmailChangeToken.create("notajwt"))
    }

    @Test
    fun `MAX_LENGTH 超過なら TooLong`() {
        val long = "a".repeat(EmailChangeToken.MAX_LENGTH + 1) + ".b.c"
        assertEquals(Err(EmailChangeToken.TooLong), EmailChangeToken.create(long))
    }

    @Test
    fun `JWT 風 3 セグメントなら成功`() {
        val jwt = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0ZXN0In0.signature"
        assertEquals(Ok(EmailChangeToken(jwt)), EmailChangeToken.create(jwt))
    }
}
