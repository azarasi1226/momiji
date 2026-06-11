package jp.momiji.domain.user

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import jp.momiji.domain.ValidationError

/**
 * 建物名・部屋番号。 任意項目（空文字 OK）。
 */
data class Building internal constructor(
    val value: String,
) {
    companion object {
        const val MAX_LENGTH = 200

        fun create(input: String): Result<Building, ValidationError> {
            if (input.length > MAX_LENGTH) return Err(TooLong)
            return Ok(Building(input))
        }
    }

    object TooLong : ValidationError("building", "建物名は $MAX_LENGTH 文字以内で入力してください")
}
