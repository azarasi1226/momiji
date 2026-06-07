package jp.momiji.domain.product

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class ProductImageUrlTest {
    @Test
    fun `null なら Ok null（任意項目）`() {
        assertEquals(Ok(null), ProductImageUrl.create(null))
    }

    @Test
    fun `空白なら Ok null（任意項目）`() {
        assertEquals(Ok(null), ProductImageUrl.create("   "))
    }

    @Test
    fun `正しいURLなら成功`() {
        val url = "https://example.com/image.png"
        assertEquals(Ok(ProductImageUrl(url)), ProductImageUrl.create(url))
    }

    @Test
    fun `URL形式でないなら InvalidFormat`() {
        assertEquals(Err(ProductImageUrl.InvalidFormat), ProductImageUrl.create("not a url"))
    }
}
