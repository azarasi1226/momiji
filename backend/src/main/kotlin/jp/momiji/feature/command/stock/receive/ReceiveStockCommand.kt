package jp.momiji.feature.command.stock.receive

import jp.momiji.domain.BusinessError
import jp.momiji.domain.stock.ReceiveStockQuantity
import jp.momiji.feature.command.CommandResult
import kotlinx.coroutines.future.await
import org.axonframework.messaging.commandhandling.gateway.CommandGateway
import org.axonframework.modelling.annotation.TargetEntityId

/** 入庫（在庫を増やす）コマンド。 在庫は商品単位なので product_id をターゲットにする。 */
data class ReceiveStockCommand(
    @TargetEntityId
    val productId: String,
    val receivedQuantity: ReceiveStockQuantity,
)

object ReceiveStockCommandResult {
    fun success() = CommandResult.success()

    fun productNotFound() = CommandResult.fail(BusinessError("商品が存在しません"))

    fun quantityOverflow() = CommandResult.fail(BusinessError("在庫数が上限を超えます"))
}

suspend fun CommandGateway.receiveStock(command: ReceiveStockCommand): CommandResult = send(command, CommandResult::class.java).await()
