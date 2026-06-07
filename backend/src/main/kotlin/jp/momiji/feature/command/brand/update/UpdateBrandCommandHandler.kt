package jp.momiji.feature.command.brand.update

import jp.momiji.domain.brand.BrandStatus
import jp.momiji.event.MomijiEventTag
import jp.momiji.event.brand.BrandArchivedEvent
import jp.momiji.event.brand.BrandCreatedEvent
import jp.momiji.event.brand.BrandUpdatedEvent
import jp.momiji.feature.command.CommandResult
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
        // 更新できるのは ACTIVE のときだけ。 未作成 (null) / アーカイブ済みは brandNotFound 扱い。
        if (state.status != BrandStatus.ACTIVE) {
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
        var status: BrandStatus?,
    ) {
        @EntityCreator
        constructor() : this(
            status = null,
        )

        @EventSourcingHandler
        fun evolve(event: BrandCreatedEvent) {
            status = BrandStatus.ACTIVE
        }

        @EventSourcingHandler
        fun evolve(event: BrandArchivedEvent) {
            status = BrandStatus.ARCHIVED
        }
    }
}
