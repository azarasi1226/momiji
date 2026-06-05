package jp.momiji.domain.brand

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import jp.momiji.domain.ValidationError

/**
 * ブランドの説明文。 任意項目なので **空文字を許す**（必須にしない）。 長さ上限だけ検証する。
 */
data class BrandDescription internal constructor(
    val value: String,
) {
    companion object {
        const val MAX_LENGTH = 5000

        fun create(input: String): Result<BrandDescription, ValidationError> {
            if (input.length > MAX_LENGTH) return Err(TooLong)
            return Ok(BrandDescription(input))
        }
    }

    object TooLong : ValidationError("description", "ブランド説明は $MAX_LENGTH 文字以内で入力してください")
}
