package jp.momiji.feature.command.stock.adjust

import jp.momiji.domain.stock.StockQuantity
import jp.momiji.event.MomijiEventTag
import jp.momiji.event.product.ProductCreatedEvent
import jp.momiji.event.stock.StockAdjustedEvent
import jp.momiji.event.stock.StockReceivedEvent
import jp.momiji.feature.command.CommandResult
import org.axonframework.eventsourcing.annotation.EventSourcingHandler
import org.axonframework.eventsourcing.annotation.reflection.EntityCreator
import org.axonframework.extension.spring.stereotype.EventSourced
import org.axonframework.messaging.commandhandling.annotation.CommandHandler
import org.axonframework.messaging.eventhandling.gateway.EventAppender
import org.axonframework.modelling.annotation.InjectEntity
import org.springframework.stereotype.Component

@Component
class AdjustStockCommandHandler {
    @CommandHandler
    fun handle(
        command: AdjustStockCommand,
        @InjectEntity state: State,
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

    @EventSourced(tagKey = MomijiEventTag.PRODUCT_ID, idType = String::class)
    class State(
        var productExists: Boolean,
        var onHand: Int,
    ) {
        @EntityCreator
        constructor() : this(
            productExists = false,
            onHand = 0,
        )

        @EventSourcingHandler
        fun evolve(event: ProductCreatedEvent) {
            productExists = true
        }

        // ProductDiscontinuedEventは考慮しない。
        // 机上の商品が消された瞬間に、物理的な在庫も消滅するなんてあり得る?
        // だから Stock と Product は独立していると考える。

        @EventSourcingHandler
        fun evolve(event: StockReceivedEvent) {
            onHand = event.onHandQuantity
        }

        @EventSourcingHandler
        fun evolve(event: StockAdjustedEvent) {
            onHand = event.onHandQuantity
        }
    }
}
