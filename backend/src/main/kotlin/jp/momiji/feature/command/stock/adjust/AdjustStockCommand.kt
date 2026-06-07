package jp.momiji.feature.command.stock.adjust

import jp.momiji.domain.BusinessError
import jp.momiji.domain.stock.StockAdjustment
import jp.momiji.feature.command.CommandResult
import kotlinx.coroutines.future.await
import org.axonframework.messaging.commandhandling.gateway.CommandGateway
import org.axonframework.modelling.annotation.TargetEntityId

/** 在庫調整（販売以外の理由で在庫を増減する）コマンド。 数量と理由の妥当な組み合わせは [StockAdjustment] が保証する。 */
data class AdjustStockCommand(
    @TargetEntityId
    val productId: String,
    val adjustment: StockAdjustment,
)

object AdjustStockCommandResult {
    fun success() = CommandResult.success()

    fun productNotFound() = CommandResult.fail(BusinessError("商品が存在しません"))

    fun stockShortage() = CommandResult.fail(BusinessError("在庫数が不足するため調整できません"))

    fun quantityOverflow() = CommandResult.fail(BusinessError("在庫数が上限を超えます"))
}

suspend fun CommandGateway.adjustStock(command: AdjustStockCommand): CommandResult = send(command, CommandResult::class.java).await()
