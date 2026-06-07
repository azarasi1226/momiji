package jp.momiji.domain.stock

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import jp.momiji.domain.ValidationError

/**
 * 在庫調整＝「符号付き数量」と「理由」の組。
 *
 * **増加（プラス調整）にできるのは [StockAdjustmentReason.canIncrease] な理由だけ**という不変条件を
 * ここで保証する（現状は棚卸し[STOCKTAKING][StockAdjustmentReason.STOCKTAKING]のみ）。
 * [create] を通さないと生成できないので、 これを持つコマンドは常に妥当な組み合わせになる。
 */
data class StockAdjustment internal constructor(
    val quantity: AdjustStockQuantity,
    val reason: StockAdjustmentReason,
) {
    companion object {
        fun create(
            quantity: AdjustStockQuantity,
            reason: StockAdjustmentReason,
        ): Result<StockAdjustment, ValidationError> {
            val isIncrease = quantity.value > 0
            if (isIncrease && !reason.canIncrease) {
                return Err(IncreaseNotAllowed)
            }
            return Ok(StockAdjustment(quantity, reason))
        }
    }

    object IncreaseNotAllowed : ValidationError("quantity", "在庫を増やす調整ができるのは棚卸しのときだけです")
}
