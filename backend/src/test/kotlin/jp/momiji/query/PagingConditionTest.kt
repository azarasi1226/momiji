package jp.momiji.query

import jp.momiji.feature.query.PagingCondition
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class PagingConditionTest {
    @Test
    fun `通常の値はそのまま`() {
        val p = PagingCondition.of(pageSize = 10, pageNumber = 3)
        assertEquals(10, p.pageSize)
        assertEquals(3, p.pageNumber)
    }

    @Test
    fun `pageSize 0 以下は既定に丸める`() {
        assertEquals(PagingCondition.DEFAULT_PAGE_SIZE, PagingCondition.of(0, 1).pageSize)
        assertEquals(PagingCondition.DEFAULT_PAGE_SIZE, PagingCondition.of(-5, 1).pageSize)
    }

    @Test
    fun `pageSize 上限超は上限に丸める`() {
        assertEquals(
            PagingCondition.MAX_PAGE_SIZE,
            PagingCondition.of(PagingCondition.MAX_PAGE_SIZE + 1, 1).pageSize,
        )
    }

    @Test
    fun `pageNumber 1 未満は 1 に丸める`() {
        assertEquals(1, PagingCondition.of(10, 0).pageNumber)
        assertEquals(1, PagingCondition.of(10, -3).pageNumber)
    }

    @Test
    fun `offset は (pageNumber - 1) * pageSize`() {
        assertEquals(0, PagingCondition.of(20, 1).offset)
        assertEquals(40, PagingCondition.of(20, 3).offset)
    }
}
