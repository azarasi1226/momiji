package jp.momiji.domain.user

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import jp.momiji.domain.DomainError

/**
 * 住所の主行 (都道府県 + 市区町村 + 町名・番地)。
 * 必須項目。
 */
data class Address1 internal constructor(
    val value: String,
) {
    companion object {
        const val MAX_LENGTH = 200

        fun create(input: String): Result<Address1, DomainError> {
            if (input.isBlank()) return Err(Blank)
            if (input.length > MAX_LENGTH) return Err(TooLong)
            return Ok(Address1(input))
        }
    }

    object Blank : DomainError("address1", "住所1 は必須です")

    object TooLong : DomainError("address1", "住所1 は $MAX_LENGTH 文字以内で入力してください")
}
