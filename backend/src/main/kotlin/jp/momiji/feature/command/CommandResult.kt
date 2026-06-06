package jp.momiji.feature.command

import jp.momiji.domain.BusinessError
import jp.momiji.domain.BusinessException

/**
 * Command の実行結果。 Axon の Command 戻り値型として CommandHandler 〜 gRPC 層を貫通する。
 *
 * 失敗時の [error] には [BusinessError] (ドメインのビジネスルール違反) が乗る。
 */
class CommandResult(
    val success: Boolean,
    val error: BusinessError? = null,
) {
    companion object {
        fun success(): CommandResult = CommandResult(success = true)

        fun fail(error: BusinessError): CommandResult = CommandResult(success = false, error = error)
    }

    override fun toString(): String = "CommandResult(success=$success, error=$error)"
}

/**
 * `CommandResult` を gRPC 層で例外に変換する bridge。
 * use case 層から domain 例外への一方向の依存。
 */
fun CommandResult.throwIfError() {
    if (!this.success) throw BusinessException(this.error!!)
}
