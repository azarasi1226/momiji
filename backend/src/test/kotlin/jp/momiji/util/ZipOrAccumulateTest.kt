package jp.momiji.util

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.get
import com.github.michaelbull.result.getError
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class ZipOrAccumulateTest {
    // 6 引数

    @Test
    fun `6引数 全て Ok なら transform を適用して Ok`() {
        val result: Result<String, List<String>> =
            zipOrAccumulate(
                { Ok(1) },
                { Ok("a") },
                { Ok(true) },
                { Ok(2) },
                { Ok("b") },
                { Ok(3) },
            ) { a, b, c, d, e, f -> "$a-$b-$c-$d-$e-$f" }

        assertEquals("1-a-true-2-b-3", result.get())
        assertNull(result.getError())
    }

    @Test
    fun `6引数 Err は fail-fast せず全件を順序どおり蓄積する`() {
        val result: Result<String, List<String>> =
            zipOrAccumulate(
                { Ok(1) },
                { Err("e2") },
                { Ok(true) },
                { Err("e4") },
                { Ok("b") },
                { Err("e6") },
            ) { _, _, _, _, _, _ -> "unused" }

        assertNull(result.get())
        assertEquals(listOf("e2", "e4", "e6"), result.getError())
    }

    // 7 引数

    @Test
    fun `7引数 全て Ok なら transform を適用して Ok`() {
        val result: Result<Int, List<String>> =
            zipOrAccumulate(
                { Ok(1) },
                { Ok(2) },
                { Ok(3) },
                { Ok(4) },
                { Ok(5) },
                { Ok(6) },
                { Ok(7) },
            ) { a, b, c, d, e, f, g -> a + b + c + d + e + f + g }

        assertEquals(28, result.get())
        assertNull(result.getError())
    }

    @Test
    fun `7引数 Err は fail-fast せず全件を順序どおり蓄積する`() {
        val result: Result<Int, List<String>> =
            zipOrAccumulate(
                { Ok(1) },
                { Err("e2") },
                { Ok(3) },
                { Ok(4) },
                { Ok(5) },
                { Err("e6") },
                { Ok(7) },
            ) { _, _, _, _, _, _, _ -> 0 }

        assertNull(result.get())
        assertEquals(listOf("e2", "e6"), result.getError())
    }

    // 9 引数

    @Test
    fun `9引数 全て Ok なら transform を適用して Ok`() {
        val result: Result<Int, List<String>> =
            zipOrAccumulate(
                { Ok(1) },
                { Ok(2) },
                { Ok(3) },
                { Ok(4) },
                { Ok(5) },
                { Ok(6) },
                { Ok(7) },
                { Ok(8) },
                { Ok(9) },
            ) { a, b, c, d, e, f, g, h, i -> a + b + c + d + e + f + g + h + i }

        assertEquals(45, result.get())
        assertNull(result.getError())
    }

    @Test
    fun `9引数 Err は fail-fast せず全件を順序どおり蓄積する`() {
        val result: Result<Int, List<String>> =
            zipOrAccumulate(
                { Err("e1") },
                { Ok(2) },
                { Ok(3) },
                { Ok(4) },
                { Ok(5) },
                { Ok(6) },
                { Ok(7) },
                { Ok(8) },
                { Err("e9") },
            ) { _, _, _, _, _, _, _, _, _ -> 0 }

        assertNull(result.get())
        assertEquals(listOf("e1", "e9"), result.getError())
    }
}
