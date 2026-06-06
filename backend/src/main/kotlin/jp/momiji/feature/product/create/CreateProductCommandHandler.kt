package jp.momiji.feature.product.create

import jp.momiji.domain.brand.BrandStatus
import jp.momiji.domain.product.ProductStatus
import jp.momiji.event.MomijiEventTag
import jp.momiji.event.brand.BrandArchivedEvent
import jp.momiji.event.brand.BrandCreatedEvent
import jp.momiji.event.eventQualifiedName
import jp.momiji.event.product.ProductCreatedEvent
import jp.momiji.feature.CommandResult
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
class CreateProductCommandHandler {
    @CommandHandler
    fun handle(
        command: CreateProductCommand,
        @InjectEntity state: State,
        eventAppender: EventAppender,
    ): CommandResult {
        if (state.productStatus != null) {
            return CreateProductCommandResult.success()
        }

        // ブランドのステータスがACTIVEでないなら商品は追加できない
        if (state.brandStatus != BrandStatus.ACTIVE) {
            return CreateProductCommandResult.brandNotFound()
        }

        eventAppender.append(
            ProductCreatedEvent(
                id = command.id,
                brandId = command.brandId,
                name = command.name.value,
                description = command.description.value,
                imageUrl = command.imageUrl?.value,
                price = command.price.value,
            ),
        )
        return CreateProductCommandResult.success()
    }

    @EventSourced(idType = CreateProductCommand.TargetId::class)
    class State(
        var productStatus: ProductStatus?,
        var brandStatus: BrandStatus?,
    ) {
        companion object {
            @JvmStatic
            @EventCriteriaBuilder
            private fun resolveCriteria(id: CreateProductCommand.TargetId): EventCriteria =
                EventCriteria.either(
                    EventCriteria
                        .havingTags(Tag.of(MomijiEventTag.PRODUCT_ID, id.productId))
                        .andBeingOneOfTypes(
                            ProductCreatedEvent::class.eventQualifiedName(),
                        ),
                    EventCriteria
                        .havingTags(Tag.of(MomijiEventTag.BRAND_ID, id.brandId))
                        .andBeingOneOfTypes(
                            BrandCreatedEvent::class.eventQualifiedName(),
                            BrandArchivedEvent::class.eventQualifiedName(),
                        ),
                )
        }

        @EntityCreator
        constructor() : this(
            productStatus = null,
            brandStatus = null,
        )

        @EventSourcingHandler
        fun evolve(event: ProductCreatedEvent) {
            productStatus = ProductStatus.ACTIVE
        }

        @EventSourcingHandler
        fun evolve(event: BrandCreatedEvent) {
            brandStatus = BrandStatus.ACTIVE
        }

        @EventSourcingHandler
        fun evolve(event: BrandArchivedEvent) {
            brandStatus = BrandStatus.ARCHIVED
        }
    }
}
