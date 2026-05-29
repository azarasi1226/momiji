package jp.momiji.domain.user

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import jp.momiji.domain.DomainError

/**
 * メール変更確認用トークン (JWT 風 3 セグメント文字列)。
 *
 * 値オブジェクトの責務は **形式チェック** だけで、 署名検証や有効期限検証は
 * [jp.momiji.feature.user.changeemail.EmailChangeTokenService] が担う。
 * これにより gRPC 入口で形式不正を即弾けて、 不正リクエストが CommandHandler まで届かないようにする。
 */
data class EmailChangeToken internal constructor(
    val value: String,
) {
    companion object {
        // 一般的な HS256 JWT は数百〜数千 byte。 余裕を持って 4096 とする。
        const val MAX_LENGTH = 4096

        // JWT 風: header.payload.signature の 3 セグメント (各セグメントにドット含まない)
        private val PATTERN = Regex("""^[^.]+\.[^.]+\.[^.]+$""")

        fun create(input: String): Result<EmailChangeToken, DomainError> {
            if (input.isBlank()) return Err(Blank)
            if (input.length > MAX_LENGTH) return Err(TooLong)
            if (!PATTERN.matches(input)) return Err(InvalidFormat)
            return Ok(EmailChangeToken(input))
        }
    }

    object Blank : DomainError("token", "トークンは必須です")

    object TooLong : DomainError("token", "トークンが長すぎます ($MAX_LENGTH 文字以内)")

    object InvalidFormat : DomainError("token", "トークンの形式が正しくありません")
}
