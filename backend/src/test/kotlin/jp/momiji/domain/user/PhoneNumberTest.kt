package jp.momiji.domain.user

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class PhoneNumberTest {
    @Test
    fun `空文字なら Invalid`() {
        assertEquals(Err(PhoneNumber.Invalid), PhoneNumber.create(""))
    }

    @Test
    fun `ハイフン無しなら Invalid`() {
        assertEquals(Err(PhoneNumber.Invalid), PhoneNumber.create("09000000000"))
    }

    @Test
    fun `文字混入なら Invalid`() {
        assertEquals(Err(PhoneNumber.Invalid), PhoneNumber.create("090-abcd-0000"))
    }

    @Test
    fun `市外局番 1 桁なら Invalid`() {
        assertEquals(Err(PhoneNumber.Invalid), PhoneNumber.create("9-0000-0000"))
    }

    @Test
    fun `通常の携帯番号なら成功`() {
        assertEquals(Ok(PhoneNumber("090-0000-0000")), PhoneNumber.create("090-0000-0000"))
    }

    @Test
    fun `固定電話 2 桁市外局番なら成功`() {
        assertEquals(Ok(PhoneNumber("03-1234-5678")), PhoneNumber.create("03-1234-5678"))
    }
}
