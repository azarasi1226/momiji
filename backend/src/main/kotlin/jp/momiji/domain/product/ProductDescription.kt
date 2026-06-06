package jp.momiji.domain.product

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import jp.momiji.domain.ValidationError

/**
 * 商品の説明文。 ブランド説明 (任意) と違い **商品では必須**（空文字を許さない）。 上限も検証する。
 */
data class ProductDescription internal constructor(
    val value: String,
) {
    companion object {
        const val MAX_LENGTH = 2000

        fun create(input: String): Result<ProductDescription, ValidationError> {
            if (input.isBlank()) return Err(Blank)
            if (input.length > MAX_LENGTH) return Err(TooLong)
            return Ok(ProductDescription(input))
        }
    }

    object Blank : ValidationError("description", "商品説明は必須です")

    object TooLong : ValidationError("description", "商品説明は $MAX_LENGTH 文字以内で入力してください")
}
