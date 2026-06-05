package jp.momiji.feature.brand.update

import jp.momiji.event.MomijiEventTag
import jp.momiji.event.brand.BrandCreatedEvent
import jp.momiji.event.brand.BrandDeletedEvent
import jp.momiji.event.brand.BrandUpdatedEvent
import jp.momiji.feature.CommandResult
import org.axonframework.eventsourcing.annotation.EventSourcingHandler
import org.axonframework.eventsourcing.annotation.reflection.EntityCreator
import org.axonframework.extension.spring.stereotype.EventSourced
import org.axonframework.messaging.commandhandling.annotation.CommandHandler
import org.axonframework.messaging.eventhandling.gateway.EventAppender
import org.axonframework.modelling.annotation.InjectEntity
import org.springframework.stereotype.Component

@Component
class UpdateBrandCommandHandler {
    @CommandHandler
    fun handle(
        command: UpdateBrandCommand,
        @InjectEntity state: State,
        eventAppender: EventAppender,
    ): CommandResult {
        if (!state.created || state.deleted) {
            return UpdateBrandCommandResult.brandNotFound()
        }

        eventAppender.append(
            BrandUpdatedEvent(
                id = command.id,
                name = command.name.value,
                description = command.description.value,
            ),
        )
        return UpdateBrandCommandResult.success()
    }

    @EventSourced(tagKey = MomijiEventTag.BRAND_ID, idType = String::class)
    class State(
        var created: Boolean,
        var deleted: Boolean,
    ) {
        @EntityCreator
        constructor() : this(
            created = false,
            deleted = false,
        )

        @EventSourcingHandler
        fun evolve(event: BrandCreatedEvent) {
            created = true
        }

        @EventSourcingHandler
        fun evolve(event: BrandDeletedEvent) {
            deleted = true
        }
    }
}
