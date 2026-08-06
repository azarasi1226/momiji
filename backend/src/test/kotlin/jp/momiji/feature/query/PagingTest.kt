package jp.momiji.feature.query

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PagingTest {
    @Test
    fun `割り切れる件数なら totalPage は商そのもの`() {
        assertEquals(2, Paging(totalCount = 40, pageSize = 20, pageNumber = 1).totalPage)
    }

    @Test
    fun `端数は切り上げる（ceil）`() {
        assertEquals(3, Paging(totalCount = 41, pageSize = 20, pageNumber = 1).totalPage)
        // 1 件でも 1 ページになる。
        assertEquals(1, Paging(totalCount = 1, pageSize = 20, pageNumber = 1).totalPage)
    }

    @Test
    fun `件数 0 なら totalPage も 0`() {
        assertEquals(0, Paging(totalCount = 0, pageSize = 20, pageNumber = 1).totalPage)
    }

    @Test
    fun `pageSize が 0 以下なら 0 で返す（ゼロ除算を避けるガード）`() {
        assertEquals(0, Paging(totalCount = 100, pageSize = 0, pageNumber = 1).totalPage)
        assertEquals(0, Paging(totalCount = 100, pageSize = -1, pageNumber = 1).totalPage)
    }
}
