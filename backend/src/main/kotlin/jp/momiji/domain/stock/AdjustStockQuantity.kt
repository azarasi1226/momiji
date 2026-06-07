package jp.momiji.domain.stock

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import jp.momiji.domain.ValidationError
import kotlin.math.abs

/**
 * 在庫の調整数（符号付き差分）。 増加は正、 減少は負。
 */
data class AdjustStockQuantity internal constructor(
    val value: Int,
) {
    companion object {
        const val MAX_MAGNITUDE = StockQuantity.MAX

        fun create(input: Int): Result<AdjustStockQuantity, ValidationError> {
            if (input == 0) return Err(Zero)
            if (abs(input) > MAX_MAGNITUDE) return Err(OutOfRange)
            return Ok(AdjustStockQuantity(input))
        }
    }

    object Zero : ValidationError("quantity", "調整数は 0 以外を入力してください")

    object OutOfRange : ValidationError("quantity", "調整数の大きさは $MAX_MAGNITUDE 以下にしてください")
}
