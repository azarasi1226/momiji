package jp.momiji.feature.command.order.ship

import jp.momiji.domain.BusinessError
import jp.momiji.feature.command.CommandResult
import kotlinx.coroutines.future.await
import org.axonframework.messaging.commandhandling.gateway.CommandGateway

/**
 * 発送手続きの「事実」を記録するコマンド（PAID → SHIPPED）。
 *
 * admin の発送手続き（gRPC、 別フェーズ）が撃つ。 発送後の完了（SHIPPED → COMPLETED）は
 * [jp.momiji.event.order.OrderShippedEvent] を拾った reactor
 * （[jp.momiji.feature.command.order.complete.CompleteShippedOrderProcessManager]）が続ける。
 * ガード（PAID のときだけ発送・発送以降は冪等）は [ShipOrderCommandHandler] が ORDER_ID の DCB sourcing で守る（ADR 0013）。
 */
data class ShipOrderCommand(
    val orderId: String,
)

object ShipOrderCommandResult {
    fun success() = CommandResult.success()

    /** PAID でも SHIPPED 以降でもない（未決済・失効・不在）注文への発送依頼。 通常 PM 経由では到達しない異常。 */
    fun cannotShip() = CommandResult.fail(BusinessError("この注文は発送できません"))
}

suspend fun CommandGateway.shipOrder(command: ShipOrderCommand): CommandResult = send(command, CommandResult::class.java).await()
