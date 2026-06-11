package jp.momiji.domain.user

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import jp.momiji.domain.ValidationError

/**
 * 市区町村。
 */
data class City internal constructor(
    val value: String,
) {
    companion object {
        const val MAX_LENGTH = 100

        fun create(input: String): Result<City, ValidationError> {
            if (input.isBlank()) return Err(Blank)
            if (input.length > MAX_LENGTH) return Err(TooLong)
            return Ok(City(input))
        }
    }

    object Blank : ValidationError("city", "市区町村は必須です")

    object TooLong : ValidationError("city", "市区町村は $MAX_LENGTH 文字以内で入力してください")
}
