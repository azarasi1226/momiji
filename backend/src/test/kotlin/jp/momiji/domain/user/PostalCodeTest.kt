package jp.momiji.domain.user

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class PostalCodeTest {
    @Test
    fun `空文字なら Invalid`() {
        assertEquals(Err(PostalCode.Invalid), PostalCode.create(""))
    }

    @Test
    fun `ハイフン無しなら Invalid`() {
        assertEquals(Err(PostalCode.Invalid), PostalCode.create("1000000"))
    }

    @Test
    fun `桁数不一致なら Invalid`() {
        assertEquals(Err(PostalCode.Invalid), PostalCode.create("100-00000"))
    }

    @Test
    fun `文字混入なら Invalid`() {
        assertEquals(Err(PostalCode.Invalid), PostalCode.create("abc-defg"))
    }

    @Test
    fun `通常の郵便番号なら成功`() {
        assertEquals(Ok(PostalCode("100-0000")), PostalCode.create("100-0000"))
    }
}
