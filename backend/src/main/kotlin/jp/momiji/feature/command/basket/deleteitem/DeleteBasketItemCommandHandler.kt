package jp.momiji.feature.command.basket.deleteitem

import jp.momiji.event.MomijiEventTag
import jp.momiji.event.basket.BasketClearedEvent
import jp.momiji.event.basket.BasketItemDeletedEvent
import jp.momiji.event.basket.BasketItemSetEvent
import jp.momiji.event.user.UserCreatedEvent
import jp.momiji.event.user.UserDeletedEvent
import jp.momiji.feature.command.CommandResult
import org.axonframework.eventsourcing.annotation.EventSourcingHandler
import org.axonframework.eventsourcing.annotation.reflection.EntityCreator
import org.axonframework.extension.spring.stereotype.EventSourced
import org.axonframework.messaging.commandhandling.annotation.CommandHandler
import org.axonframework.messaging.eventhandling.gateway.EventAppender
import org.axonframework.modelling.annotation.InjectEntity
import org.springframework.stereotype.Component

@Component
class DeleteBasketItemCommandHandler {
    @CommandHandler
    fun handle(
        command: DeleteBasketItemCommand,
        @InjectEntity state: State,
        eventAppender: EventAppender,
    ): CommandResult {
        if (!state.userExists) {
            return DeleteBasketItemCommandResult.userNotFound()
        }
        // 冪等性: そもそもカゴに無いなら何もせず成功
        if (command.productId !in state.itemProductIds) {
            return DeleteBasketItemCommandResult.success()
        }

        eventAppender.append(
            BasketItemDeletedEvent(
                userId = command.userId,
                productId = command.productId,
            ),
        )
        return DeleteBasketItemCommandResult.success()
    }

    @EventSourced(tagKey = MomijiEventTag.USER_ID, idType = String::class)
    class State(
        var userExists: Boolean,
        val itemProductIds: MutableSet<String>,
    ) {
        @EntityCreator
        constructor() : this(
            userExists = false,
            itemProductIds = mutableSetOf(),
        )

        @EventSourcingHandler
        fun evolve(event: UserCreatedEvent) {
            userExists = true
        }

        @EventSourcingHandler
        fun evolve(event: UserDeletedEvent) {
            userExists = false
        }

        @EventSourcingHandler
        fun evolve(event: BasketItemSetEvent) {
            itemProductIds.add(event.productId)
        }

        @EventSourcingHandler
        fun evolve(event: BasketItemDeletedEvent) {
            itemProductIds.remove(event.productId)
        }

        @EventSourcingHandler
        fun evolve(event: BasketClearedEvent) {
            itemProductIds.clear()
        }
    }
}
