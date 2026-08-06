package jp.momiji.domain.user

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class DeliveryNoteTest {
    @Test
    fun `空文字は任意項目なので成功`() {
        assertEquals(Ok(DeliveryNote("")), DeliveryNote.create(""))
    }

    @Test
    fun `上限ちょうど（MAX_LENGTH 文字）なら成功`() {
        val input = "a".repeat(DeliveryNote.MAX_LENGTH)
        assertEquals(Ok(DeliveryNote(input)), DeliveryNote.create(input))
    }

    @Test
    fun `上限超過なら TooLong`() {
        assertEquals(Err(DeliveryNote.TooLong), DeliveryNote.create("a".repeat(DeliveryNote.MAX_LENGTH + 1)))
    }
}
