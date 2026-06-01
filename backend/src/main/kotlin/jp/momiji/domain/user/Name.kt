package jp.momiji.domain.user

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import jp.momiji.domain.ValidationError

data class Name internal constructor(
    val value: String,
) {
    companion object {
        const val MAX_LENGTH = 100

        fun create(input: String): Result<Name, ValidationError> {
            if (input.isBlank()) return Err(Blank)
            if (input.length > MAX_LENGTH) return Err(TooLong)
            return Ok(Name(input))
        }
    }

    object Blank : ValidationError("name", "名前は必須です")

    object TooLong : ValidationError("name", "名前は $MAX_LENGTH 文字以内で入力してください")
}
