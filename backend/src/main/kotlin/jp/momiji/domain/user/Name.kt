package jp.momiji.domain.user

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import jp.momiji.domain.DomainError

// Axon が Command を byte[] 経由で Jackson serde するため、 @JvmInline value class +
// private constructor だと deserialize に失敗する。 data class + internal constructor
// にして、 「外部 (UI/gRPC 層) からは create() しか使えない」 + 「テスト・同モジュール内では
// 直接インスタンス化可能」 + 「Jackson の primary constructor 経由 deserialize は通る」 を両立する。
data class Name internal constructor(
    val value: String,
) {
    companion object {
        const val MAX_LENGTH = 100

        fun create(input: String): Result<Name, DomainError> {
            if (input.isBlank()) return Err(Blank)
            if (input.length > MAX_LENGTH) return Err(TooLong)
            return Ok(Name(input))
        }
    }

    // nested object を companion object の中に置くと `Name.Blank` で参照できなくなる
    // (Kotlin の companion shortcut は nested object 定義には効かない)。 class 直下に置く。
    object Blank : DomainError("name", "名前は必須です")

    object TooLong : DomainError("name", "名前は $MAX_LENGTH 文字以内で入力してください")
}
