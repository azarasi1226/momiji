package jp.momiji.domain.user

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class CityTest {
    @Test
    fun `空文字なら Blank`() {
        assertEquals(Err(City.Blank), City.create(""))
    }

    @Test
    fun `空白のみなら Blank`() {
        assertEquals(Err(City.Blank), City.create("   "))
    }

    @Test
    fun `上限超過なら TooLong`() {
        assertEquals(Err(City.TooLong), City.create("a".repeat(City.MAX_LENGTH + 1)))
    }

    @Test
    fun `上限ちょうど（MAX_LENGTH 文字）なら成功`() {
        val input = "a".repeat(City.MAX_LENGTH)
        assertEquals(Ok(City(input)), City.create(input))
    }

    @Test
    fun `通常の市区町村なら成功`() {
        assertEquals(Ok(City("千代田区")), City.create("千代田区"))
    }
}
