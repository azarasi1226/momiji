package jp.momiji.domain.stock

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import jp.momiji.domain.ValidationError

/**
 * 1 回の入庫数。 1 〜 100 万。 0 や負数は入庫として無意味なので弾く。
 */
data class ReceiveStockQuantity internal constructor(
    val value: Int,
) {
    companion object {
        const val MIN = 1
        const val MAX = StockQuantity.MAX

        fun create(input: Int): Result<ReceiveStockQuantity, ValidationError> {
            if (input !in MIN..MAX) return Err(OutOfRange)
            return Ok(ReceiveStockQuantity(input))
        }
    }

    object OutOfRange : ValidationError("quantity", "入庫数は $MIN 〜 $MAX の範囲で入力してください")
}
