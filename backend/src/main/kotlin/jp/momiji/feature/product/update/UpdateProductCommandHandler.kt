package jp.momiji.feature.product.update

import jp.momiji.domain.product.ProductStatus
import jp.momiji.event.MomijiEventTag
import jp.momiji.event.product.ProductCreatedEvent
import jp.momiji.event.product.ProductDiscontinuedEvent
import jp.momiji.event.product.ProductUpdatedEvent
import jp.momiji.feature.CommandResult
import org.axonframework.eventsourcing.annotation.EventSourcingHandler
import org.axonframework.eventsourcing.annotation.reflection.EntityCreator
import org.axonframework.extension.spring.stereotype.EventSourced
import org.axonframework.messaging.commandhandling.annotation.CommandHandler
import org.axonframework.messaging.eventhandling.gateway.EventAppender
import org.axonframework.modelling.annotation.InjectEntity
import org.springframework.stereotype.Component

@Component
class UpdateProductCommandHandler {
    @CommandHandler
    fun handle(
        command: UpdateProductCommand,
        @InjectEntity state: State,
        eventAppender: EventAppender,
    ): CommandResult {
        // 更新できるのは ACTIVE のときだけ。 未作成 (null) / 廃番済みは productNotFound 扱い。
        if (state.status != ProductStatus.ACTIVE) {
            return UpdateProductCommandResult.productNotFound()
        }

        eventAppender.append(
            ProductUpdatedEvent(
                id = command.id,
                name = command.name.value,
                description = command.description.value,
                imageUrl = command.imageUrl?.value,
                price = command.price.value,
            ),
        )
        return UpdateProductCommandResult.success()
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
