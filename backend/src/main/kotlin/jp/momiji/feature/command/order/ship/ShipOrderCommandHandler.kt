package jp.momiji.feature.command.order.ship

import jp.momiji.event.order.OrderShippedEvent
import jp.momiji.feature.command.CommandResult
import jp.momiji.feature.command.order.OrderState
import org.axonframework.messaging.commandhandling.annotation.CommandHandler
import org.axonframework.messaging.eventhandling.gateway.EventAppender
import org.axonframework.modelling.annotation.InjectEntity
import org.springframework.stereotype.Component

/**
 * 発送手続きの記録 CommandHandler。 **PAID のときだけ** SHIPPED へ遷移させる。
 *
 * ガードを整合境界（ここ）に集約する（ADR 0013）:
 * - **SHIPPED 以降** → no-op 成功（発送依頼の重複・再送を冪等に握る）。
 * - **PAID** → [OrderShippedEvent] を発行して SHIPPED に。
 * - **それ以外（未決済・失効・不在）** → [ShipOrderCommandResult.cannotShip]。 PM 経由では到達しないが、
 *   別経路・遅延・順序前後への防御として弾く。
 */
@Component
class ShipOrderCommandHandler {
    @CommandHandler
    fun handle(
        command: ShipOrderCommand,
        @InjectEntity(idProperty = "orderId") order: OrderState,
        eventAppender: EventAppender,
    ): CommandResult {
        // 冪等性: 既に発送済み or それ以降なら no-op 成功。
        if (order.isShippedOrBeyond) {
            return ShipOrderCommandResult.success()
        }
        // PAID のときだけ発送手続きを記録する。
        if (order.isPaid) {
            eventAppender.append(OrderShippedEvent(orderId = command.orderId))
            return ShipOrderCommandResult.success()
        }

        // 未決済・失効・不在 → 発送不能。
        return ShipOrderCommandResult.cannotShip()
    }
}
