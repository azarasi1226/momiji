package jp.momiji.feature.command.product.discontinue

import jp.momiji.domain.product.ProductStatus
import jp.momiji.event.MomijiEventTag
import jp.momiji.event.product.ProductCreatedEvent
import jp.momiji.event.product.ProductDiscontinuedEvent
import jp.momiji.feature.command.CommandResult
import org.axonframework.eventsourcing.annotation.EventSourcingHandler
import org.axonframework.eventsourcing.annotation.reflection.EntityCreator
import org.axonframework.extension.spring.stereotype.EventSourced
import org.axonframework.messaging.commandhandling.annotation.CommandHandler
import org.axonframework.messaging.eventhandling.gateway.EventAppender
import org.axonframework.modelling.annotation.InjectEntity
import org.springframework.stereotype.Component

@Component
class DiscontinueProductCommandHandler {
    @CommandHandler
    fun handle(
        command: DiscontinueProductCommand,
        @InjectEntity state: State,
        eventAppender: EventAppender,
    ): CommandResult {
        when (state.status) {
            null -> return DiscontinueProductCommandResult.productNotFound()
            // 冪等性: すでに廃番済みならイベントを出さず成功を返す
            ProductStatus.DISCONTINUED -> return DiscontinueProductCommandResult.success()
            ProductStatus.ACTIVE -> {
                eventAppender.append(
                    ProductDiscontinuedEvent(
                        id = command.id,
                    ),
                )
                return DiscontinueProductCommandResult.success()
            }
        }
    }

    @EventSourced(tagKey = MomijiEventTag.PRODUCT_ID, idType = String::class)
    class State(
        var status: ProductStatus?,
    ) {
        @EntityCreator
        constructor() : this(
            status = null,
        )

        @EventSourcingHandler
        fun evolve(event: ProductCreatedEvent) {
            status = ProductStatus.ACTIVE
        }

        @EventSourcingHandler
        fun evolve(event: ProductDiscontinuedEvent) {
            status = ProductStatus.DISCONTINUED
        }
    }
}
