package jp.momiji.domain.user

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import jp.momiji.domain.ValidationError

/**
 * 配達メモ（置き配・宅配ボックス等のドライバーへの配達指示）。 任意項目（空文字 OK）。
 */
data class DeliveryNote internal constructor(
    val value: String,
) {
    companion object {
        const val MAX_LENGTH = 500

        fun create(input: String): Result<DeliveryNote, ValidationError> {
            if (input.length > MAX_LENGTH) return Err(TooLong)
            return Ok(DeliveryNote(input))
        }
    }

    object TooLong : ValidationError("deliveryNote", "配達メモは $MAX_LENGTH 文字以内で入力してください")
}
