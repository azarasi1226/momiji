package jp.momiji.domain.product

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import jp.momiji.domain.ValidationError

/**
 * 商品価格（円・税込整数）。 1 円以上、 10 億円以下。
 */
data class ProductPrice internal constructor(
    val value: Int,
) {
    companion object {
        const val MIN = 1
        const val MAX = 1_000_000_000

        fun create(input: Int): Result<ProductPrice, ValidationError> {
            if (input !in MIN..MAX) return Err(OutOfRange)
            return Ok(ProductPrice(input))
        }
    }

    object OutOfRange : ValidationError("price", "商品価格は $MIN 〜 $MAX の範囲で入力してください")
}
