package jp.momiji.feature.command.basket.setitem

import jp.momiji.domain.product.ProductStatus
import jp.momiji.event.MomijiEventTag
import jp.momiji.event.basket.BasketClearedEvent
import jp.momiji.event.basket.BasketItemDeletedEvent
import jp.momiji.event.basket.BasketItemSetEvent
import jp.momiji.event.eventQualifiedName
import jp.momiji.event.product.ProductCreatedEvent
import jp.momiji.event.product.ProductDiscontinuedEvent
import jp.momiji.event.user.UserCreatedEvent
import jp.momiji.event.user.UserDeletedEvent
import jp.momiji.feature.command.CommandResult
import org.axonframework.eventsourcing.annotation.EventCriteriaBuilder
import org.axonframework.eventsourcing.annotation.EventSourcingHandler
import org.axonframework.eventsourcing.annotation.reflection.EntityCreator
import org.axonframework.extension.spring.stereotype.EventSourced
import org.axonframework.messaging.commandhandling.annotation.CommandHandler
import org.axonframework.messaging.eventhandling.gateway.EventAppender
import org.axonframework.messaging.eventstreaming.EventCriteria
import org.axonframework.messaging.eventstreaming.Tag
import org.axonframework.modelling.annotation.InjectEntity
import org.springframework.stereotype.Component

@Component
class SetBasketItemCommandHandler {
    @CommandHandler
    fun handle(
        command: SetBasketItemCommand,
        @InjectEntity state: State,
        eventAppender: EventAppender,
    ): CommandResult {
        if (!state.userExists) {
            return SetBasketItemCommandResult.userNotFound()
        }
        // 生産終了商品はカゴに入れられない。 ACTIVE のみ許可。
        if (state.productStatus != ProductStatus.ACTIVE) {
            return SetBasketItemCommandResult.productNotFound()
        }
        // 既にカゴにある商品の個数変更は上限に数えない。 新規追加のときだけ種類数の上限を見る。
        val isNewProduct = command.productId !in state.itemProductIds
        if (isNewProduct && state.itemProductIds.size >= MAX_ITEM_KIND_COUNT) {
            return SetBasketItemCommandResult.productMaxKindOver()
        }

        eventAppender.append(
            BasketItemSetEvent(
                userId = command.userId,
                productId = command.productId,
                itemQuantity = command.itemQuantity.value,
            ),
        )
        return SetBasketItemCommandResult.success()
    }

    /**
     * user_id と product_id の **2 タグをまたぐ** DCB State。
     * - user_id: ユーザー存在（UserCreated/Deleted）+ カゴの中身（BasketItemSet/Deleted/Cleared）
     * - product_id: 商品の状態（ProductCreated → ACTIVE / ProductDiscontinued → DISCONTINUED）
     */
    @EventSourced(idType = SetBasketItemCommand.TargetId::class)
    class State(
        var userExists: Boolean,
        var productStatus: ProductStatus?,
        val itemProductIds: MutableSet<String>,
    ) {
        companion object {
            @JvmStatic
            @EventCriteriaBuilder
            private fun resolveCriteria(id: SetBasketItemCommand.TargetId): EventCriteria =
                EventCriteria.either(
                    EventCriteria
                        .havingTags(Tag.of(MomijiEventTag.USER_ID, id.userId))
                        .andBeingOneOfTypes(
                            UserCreatedEvent::class.eventQualifiedName(),
                            UserDeletedEvent::class.eventQualifiedName(),
                            BasketItemSetEvent::class.eventQualifiedName(),
                            BasketItemDeletedEvent::class.eventQualifiedName(),
                            BasketClearedEvent::class.eventQualifiedName(),
                        ),
                    EventCriteria
                        .havingTags(Tag.of(MomijiEventTag.PRODUCT_ID, id.productId))
                        .andBeingOneOfTypes(
                            ProductCreatedEvent::class.eventQualifiedName(),
                            ProductDiscontinuedEvent::class.eventQualifiedName(),
                        ),
                )
        }

        @EntityCreator
        constructor() : this(
            userExists = false,
            productStatus = null,
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
        fun evolve(event: ProductCreatedEvent) {
            productStatus = ProductStatus.ACTIVE
        }

        @EventSourcingHandler
        fun evolve(event: ProductDiscontinuedEvent) {
            productStatus = ProductStatus.DISCONTINUED
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

    companion object {
        const val MAX_ITEM_KIND_COUNT = 50
    }
}
