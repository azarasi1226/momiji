package jp.momiji.domain.product

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import jp.momiji.domain.ValidationError

data class ProductName internal constructor(
    val value: String,
) {
    companion object {
        const val MAX_LENGTH = 200

        fun create(input: String): Result<ProductName, ValidationError> {
            if (input.isBlank()) return Err(Blank)
            if (input.length > MAX_LENGTH) return Err(TooLong)
            return Ok(ProductName(input))
        }
    }

    object Blank : ValidationError("name", "商品名は必須です")

    object TooLong : ValidationError("name", "商品名は $MAX_LENGTH 文字以内で入力してください")
}
