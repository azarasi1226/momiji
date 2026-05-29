package jp.momiji.domain.user

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import jp.momiji.domain.DomainError

/**
 * 住所の補助行 (建物名・部屋番号 等)。
 * 任意項目 (空文字 OK)。
 */
data class Address2 internal constructor(
    val value: String,
) {
    companion object {
        const val MAX_LENGTH = 200

        fun create(input: String): Result<Address2, DomainError> {
            if (input.length > MAX_LENGTH) return Err(TooLong)
            return Ok(Address2(input))
        }

        object TooLong : DomainError("address2", "住所2 は $MAX_LENGTH 文字以内で入力してください")
    }
}
