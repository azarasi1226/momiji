package jp.momiji.domain.user

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import jp.momiji.domain.ValidationError

data class Email internal constructor(
    val value: String,
) {
    companion object {
        // RFC 5321 上限 (local-part 64 + @ + domain 255 で実質 320 だが、 実運用 254 が一般的)
        const val MAX_LENGTH = 254

        // 簡易チェック: local@domain でドメインにドットを 1 つ以上含む
        private val PATTERN = Regex("""^[^\s@]+@[^\s@]+\.[^\s@]+$""")

        fun create(input: String): Result<Email, ValidationError> {
            // Emailは大文字・小文字区別せずメールを飛ばせる。けどこのシステムでは小文字で統一して保存する。
            // DBのユニーク制約でしか区別できないため、正規化してからバリデーションする。
            val normalized = input.trim().lowercase()
            if (normalized.isBlank()) return Err(Blank)
            if (normalized.length > MAX_LENGTH) return Err(TooLong)
            if (!PATTERN.matches(normalized)) return Err(Invalid)
            return Ok(Email(normalized))
        }
    }

    object Blank : ValidationError("email", "メールアドレスは必須です")

    object TooLong : ValidationError("email", "メールアドレスは $MAX_LENGTH 文字以内で入力してください")

    object Invalid : ValidationError("email", "メールアドレスの形式が正しくありません")
}
