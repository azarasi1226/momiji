package jp.momiji.feature.brand.create

import jp.momiji.event.MomijiEventTag
import jp.momiji.event.brand.BrandCreatedEvent
import jp.momiji.feature.CommandResult
import org.axonframework.eventsourcing.annotation.EventSourcingHandler
import org.axonframework.eventsourcing.annotation.reflection.EntityCreator
import org.axonframework.extension.spring.stereotype.EventSourced
import org.axonframework.messaging.commandhandling.annotation.CommandHandler
import org.axonframework.messaging.eventhandling.gateway.EventAppender
import org.axonframework.modelling.annotation.InjectEntity
import org.springframework.stereotype.Component

@Component
class CreateBrandCommandHandler {
    @CommandHandler
    fun handle(
        command: CreateBrandCommand,
        @InjectEntity state: State,
        eventAppender: EventAppender,
    ): CommandResult {
        // 冪等性: id は BFF が採番して渡す。 同じ id での再送 (リトライ) は
        // 新規イベントを出さず success を返す（ブランドの二重作成を防ぐ）。
        if (state.created) {
            return CreateBrandCommandResult.success()
        }

        eventAppender.append(
            BrandCreatedEvent(
                id = command.id,
                name = command.name.value,
                description = command.description.value,
            ),
        )
        return CreateBrandCommandResult.success()
    }

    @EventSourced(tagKey = MomijiEventTag.BRAND_ID, idType = String::class)
    class State(
        var created: Boolean,
    ) {
        @EntityCreator
        constructor() : this(
            created = false,
        )

        @EventSourcingHandler
        fun evolve(event: BrandCreatedEvent) {
            created = true
        }
    }
}
