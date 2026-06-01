package jp.momiji.domain.user

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import jp.momiji.domain.ValidationError

data class EmailChangeToken internal constructor(
    val value: String,
) {
    companion object {
        // 一般的な HS256 JWT は数百〜数千 byte。 余裕を持って 4096 とする。
        const val MAX_LENGTH = 4096

        // JWT 風: header.payload.signature の 3 セグメント (各セグメントにドット含まない)
        private val PATTERN = Regex("""^[^.]+\.[^.]+\.[^.]+$""")

        fun create(input: String): Result<EmailChangeToken, ValidationError> {
            if (input.isBlank()) return Err(Blank)
            if (input.length > MAX_LENGTH) return Err(TooLong)
            if (!PATTERN.matches(input)) return Err(InvalidFormat)
            return Ok(EmailChangeToken(input))
        }
    }

    object Blank : ValidationError("token", "トークンは必須です")

    object TooLong : ValidationError("token", "トークンが長すぎます ($MAX_LENGTH 文字以内)")

    object InvalidFormat : ValidationError("token", "トークンの形式が正しくありません")
}
