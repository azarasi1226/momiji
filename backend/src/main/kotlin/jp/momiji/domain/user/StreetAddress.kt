package jp.momiji.domain.user

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import jp.momiji.domain.ValidationError

/**
 * 番地（町域・丁目・番・号）。
 */
data class StreetAddress internal constructor(
    val value: String,
) {
    companion object {
        const val MAX_LENGTH = 200

        fun create(input: String): Result<StreetAddress, ValidationError> {
            if (input.isBlank()) return Err(Blank)
            if (input.length > MAX_LENGTH) return Err(TooLong)
            return Ok(StreetAddress(input))
        }
    }

    object Blank : ValidationError("streetAddress", "番地は必須です")

    object TooLong : ValidationError("streetAddress", "番地は $MAX_LENGTH 文字以内で入力してください")
}
