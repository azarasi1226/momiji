package jp.momiji.domain

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class UlidTest {
    @Test
    fun `正しいULIDなら成功し入力をそのまま返す`() {
        val ulid = "01ARZ3NDEKTSV4RRFFQ69G5FAV"
        assertEquals(Ok(ulid), Ulid.validate(ulid))
    }

    @Test
    fun `空文字なら Invalid`() {
        assertEquals(Err(Ulid.Invalid), Ulid.validate(""))
    }

    @Test
    fun `ULID形式でない文字列なら Invalid`() {
        assertEquals(Err(Ulid.Invalid), Ulid.validate("not-a-ulid"))
    }

    @Test
    fun `長さが足りない (25文字) なら Invalid`() {
        assertEquals(Err(Ulid.Invalid), Ulid.validate("01ARZ3NDEKTSV4RRFFQ69G5FA"))
    }

    @Test
    fun `base32 にない記号を含むなら Invalid`() {
        assertEquals(Err(Ulid.Invalid), Ulid.validate("01ARZ3NDEKTSV4RRFFQ69G5FA!"))
    }
}
