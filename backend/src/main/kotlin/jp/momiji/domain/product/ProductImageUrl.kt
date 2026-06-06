package jp.momiji.domain.product

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import jp.momiji.domain.ValidationError
import java.net.URI

/**
 * 商品画像 URL。 **任意項目**。 未指定 (null / 空白) は許可し、 その場合 [create] は `Ok(null)` を返す。
 * 値がある場合のみ URL 形式を検証する。
 */
data class ProductImageUrl internal constructor(
    val value: String,
) {
    companion object {
        /**
         * 任意項目のため戻り値は `ProductImageUrl?`。 未指定は `Ok(null)`、 不正形式のみ [InvalidFormat]。
         */
        fun create(input: String?): Result<ProductImageUrl?, ValidationError> {
            if (input.isNullOrBlank()) return Ok(null)
            return try {
                URI(input).toURL()
                Ok(ProductImageUrl(input))
            } catch (_: Exception) {
                Err(InvalidFormat)
            }
        }
    }

    object InvalidFormat : ValidationError("imageUrl", "商品画像URLはURL形式である必要があります")
}
