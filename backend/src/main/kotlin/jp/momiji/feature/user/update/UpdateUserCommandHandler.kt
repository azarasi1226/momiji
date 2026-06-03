package jp.momiji.feature.user.update

import jp.momiji.event.MomijiEventTag
import jp.momiji.event.user.UserCreatedEvent
import jp.momiji.event.user.UserDeletedEvent
import jp.momiji.event.user.UserUpdatedEvent
import jp.momiji.feature.CommandResult
import org.axonframework.eventsourcing.annotation.EventSourcingHandler
import org.axonframework.eventsourcing.annotation.reflection.EntityCreator
import org.axonframework.extension.spring.stereotype.EventSourced
import org.axonframework.messaging.commandhandling.annotation.CommandHandler
import org.axonframework.messaging.eventhandling.gateway.EventAppender
import org.axonframework.modelling.annotation.InjectEntity
import org.springframework.stereotype.Component

@Component
class UpdateUserCommandHandler {
    @CommandHandler
    fun handle(
        command: UpdateUserCommand,
        @InjectEntity state: State,
        eventAppender: EventAppender,
    ): CommandResult {
        if (!state.created || state.deleted) {
            return UpdateUserCommandResult.userNotFound()
        }

        eventAppender.append(
            UserUpdatedEvent(
                id = command.id,
                name = command.name.value,
                phoneNumber = command.phoneNumber.value,
                postalCode = command.postalCode.value,
                address1 = command.address1.value,
                address2 = command.address2.value,
            ),
        )
        return UpdateUserCommandResult.success()
    }

    @EventSourced(tagKey = MomijiEventTag.USER_ID, idType = String::class)
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
        fun evolve(event: UserCreatedEvent) {
            created = true
        }

        @EventSourcingHandler
        fun evolve(event: UserDeletedEvent) {
            deleted = true
        }
    }
}
