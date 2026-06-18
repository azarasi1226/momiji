package jp.momiji.feature.command.stock.adjust

import jp.momiji.domain.stock.StockQuantity
import jp.momiji.event.stock.StockAdjustedEvent
import jp.momiji.feature.command.CommandResult
import jp.momiji.feature.command.stock.ProductStockState
import org.axonframework.messaging.commandhandling.annotation.CommandHandler
import org.axonframework.messaging.eventhandling.gateway.EventAppender
import org.axonframework.modelling.annotation.InjectEntity
import org.springframework.stereotype.Component

@Component
class AdjustStockCommandHandler {
    @CommandHandler
    fun handle(
        command: AdjustStockCommand,
        @InjectEntity state: ProductStockState,
        eventAppender: EventAppender,
    ): CommandResult {
        if (!state.productExists) {
            return AdjustStockCommandResult.productNotFound()
        }
        val resulting = state.onHand + command.adjustment.quantity.value
        if (resulting < StockQuantity.MIN) {
            return AdjustStockCommandResult.stockShortage()
        }
        if (resulting > StockQuantity.MAX) {
            return AdjustStockCommandResult.quantityOverflow()
        }

        eventAppender.append(
            StockAdjustedEvent(
                productId = command.productId,
                adjustmentQuantity = command.adjustment.quantity.value,
                reason = command.adjustment.reason.name,
                onHandQuantity = resulting,
            ),
        )
        return AdjustStockCommandResult.success()
    }
}
