package jp.momiji.feature.command.stock.receive

import jp.momiji.domain.stock.StockQuantity
import jp.momiji.event.stock.StockReceivedEvent
import jp.momiji.feature.command.CommandResult
import jp.momiji.feature.command.stock.ProductStockState
import org.axonframework.messaging.commandhandling.annotation.CommandHandler
import org.axonframework.messaging.eventhandling.gateway.EventAppender
import org.axonframework.modelling.annotation.InjectEntity
import org.springframework.stereotype.Component

@Component
class ReceiveStockCommandHandler {
    @CommandHandler
    fun handle(
        command: ReceiveStockCommand,
        @InjectEntity state: ProductStockState,
        eventAppender: EventAppender,
    ): CommandResult {
        if (!state.productExists) {
            return ReceiveStockCommandResult.productNotFound()
        }
        val resulting = state.onHand + command.receivedQuantity.value
        if (resulting > StockQuantity.MAX) {
            return ReceiveStockCommandResult.quantityOverflow()
        }

        eventAppender.append(
            StockReceivedEvent(
                productId = command.productId,
                receivedQuantity = command.receivedQuantity.value,
                onHandQuantity = resulting,
            ),
        )
        return ReceiveStockCommandResult.success()
    }
}
