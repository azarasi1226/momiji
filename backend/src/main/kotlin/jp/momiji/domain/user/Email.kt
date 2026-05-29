package jp.momiji.domain.user

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import jp.momiji.domain.DomainError

data class Email internal constructor(
    val value: String,
) {
    companion object {
        // RFC 5321 上限 (local-part 64 + @ + domain 255 で実質 320 だが、 実運用 254 が一般的)
        const val MAX_LENGTH = 254

        // 簡易チェック: local@domain でドメインにドットを 1 つ以上含む
        private val PATTERN = Regex("""^[^\s@]+@[^\s@]+\.[^\s@]+$""")

        fun create(input: String): Result<Email, DomainError> {
            if (input.isBlank()) return Err(Blank)
            if (input.length > MAX_LENGTH) return Err(TooLong)
            if (!PATTERN.matches(input)) return Err(Invalid)
            return Ok(Email(input))
        }
    }

    object Blank : DomainError("email", "メールアドレスは必須です")

    object TooLong : DomainError("email", "メールアドレスは $MAX_LENGTH 文字以内で入力してください")

    object Invalid : DomainError("email", "メールアドレスの形式が正しくありません")
}
