package jp.momiji.domain.basket

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import jp.momiji.domain.ValidationError

/**
 * 買い物かごの 1 商品あたりの個数。 1〜99。 0 は「カゴから消す」を意味するので最小は 1。
 */
data class BasketItemQuantity internal constructor(
    val value: Int,
) {
    companion object {
        const val MIN = 1
        const val MAX = 99

        fun create(input: Int): Result<BasketItemQuantity, ValidationError> {
            if (input !in MIN..MAX) return Err(OutOfRange)
            return Ok(BasketItemQuantity(input))
        }
    }

    object OutOfRange : ValidationError("itemQuantity", "個数は $MIN 〜 $MAX の範囲で入力してください")
}
