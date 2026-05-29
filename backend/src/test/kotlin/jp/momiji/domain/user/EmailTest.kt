package jp.momiji.domain.user

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class EmailTest {
    @Test
    fun `空文字なら Blank`() {
        assertEquals(Err(Email.Blank), Email.create(""))
    }

    @Test
    fun `空白のみなら Blank`() {
        assertEquals(Err(Email.Blank), Email.create("   "))
    }

    @Test
    fun `アットマーク無しなら Invalid`() {
        assertEquals(Err(Email.Invalid), Email.create("alice.example.com"))
    }

    @Test
    fun `ドメインにドット無しなら Invalid`() {
        assertEquals(Err(Email.Invalid), Email.create("alice@localhost"))
    }

    @Test
    fun `アットマーク 2 つ含めば Invalid`() {
        assertEquals(Err(Email.Invalid), Email.create("alice@@example.com"))
    }

    @Test
    fun `空白を含めば Invalid`() {
        assertEquals(Err(Email.Invalid), Email.create("ali ce@example.com"))
    }

    @Test
    fun `境界 + 1 (255 文字) なら TooLong`() {
        // 254 文字制限を超える long email を作る
        val input = "a".repeat(Email.MAX_LENGTH + 1 - "@example.com".length) + "@example.com"
        assertEquals(Err(Email.TooLong), Email.create(input))
    }

    @Test
    fun `通常のメールアドレスなら成功`() {
        assertEquals(Ok(Email("alice@example.com")), Email.create("alice@example.com"))
    }

    @Test
    fun `サブドメイン付きでも成功`() {
        assertEquals(Ok(Email("alice@mail.example.co.jp")), Email.create("alice@mail.example.co.jp"))
    }
}
