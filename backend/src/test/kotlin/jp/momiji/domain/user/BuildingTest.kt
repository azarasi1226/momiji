package jp.momiji.domain.user

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class BuildingTest {
    @Test
    fun `空文字は任意項目なので成功`() {
        assertEquals(Ok(Building("")), Building.create(""))
    }

    @Test
    fun `上限ちょうど（MAX_LENGTH 文字）なら成功`() {
        val input = "a".repeat(Building.MAX_LENGTH)
        assertEquals(Ok(Building(input)), Building.create(input))
    }

    @Test
    fun `上限超過なら TooLong`() {
        assertEquals(Err(Building.TooLong), Building.create("a".repeat(Building.MAX_LENGTH + 1)))
    }
}
