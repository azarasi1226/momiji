package jp.momiji.domain.stock

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import jp.momiji.domain.ValidationError

/**
 * 物理在庫数（onHand）。 0 〜 100 万。
 */
data class StockQuantity internal constructor(
    val value: Int,
) {
    companion object {
        const val MIN = 0
        const val MAX = 1_000_000

        fun create(input: Int): Result<StockQuantity, ValidationError> {
            if (input !in MIN..MAX) return Err(OutOfRange)
            return Ok(StockQuantity(input))
        }
    }

    object OutOfRange : ValidationError("stockQuantity", "在庫数は $MIN 〜 $MAX の範囲です")
}
