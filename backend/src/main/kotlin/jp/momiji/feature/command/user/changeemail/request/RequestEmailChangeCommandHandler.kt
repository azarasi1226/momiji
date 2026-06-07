package jp.momiji.feature.command.user.changeemail.request

import iss.jooq.generated.tables.references.LOOKUP_EMAIL
import jp.momiji.event.MomijiEventTag
import jp.momiji.event.user.EmailChangeRequestedEvent
import jp.momiji.event.user.UserCreatedEvent
import jp.momiji.event.user.UserDeletedEvent
import jp.momiji.feature.command.CommandResult
import org.axonframework.eventsourcing.annotation.EventSourcingHandler
import org.axonframework.eventsourcing.annotation.reflection.EntityCreator
import org.axonframework.extension.spring.stereotype.EventSourced
import org.axonframework.messaging.commandhandling.annotation.CommandHandler
import org.axonframework.messaging.eventhandling.gateway.EventAppender
import org.axonframework.modelling.annotation.InjectEntity
import org.jooq.DSLContext
import org.springframework.stereotype.Component

@Component
class RequestEmailChangeCommandHandler(
    private val dsl: DSLContext,
) {
    @CommandHandler
    fun handle(
        command: RequestEmailChangeCommand,
        @InjectEntity state: State,
        eventAppender: EventAppender,
    ): CommandResult {
        if (!state.created || state.deleted) {
            return RequestEmailChangeCommandResult.userNotFound()
        }

        if (emailAlreadyExists(command.newEmail.value)) {
            return RequestEmailChangeCommandResult.emailAlreadyInUse()
        }

        eventAppender.append(
            // Event は将来のスキーマ進化のためあえて String のまま。
            EmailChangeRequestedEvent(
                userId = command.userId,
                newEmail = command.newEmail.value,
            ),
        )
        return RequestEmailChangeCommandResult.success()
    }

    private fun emailAlreadyExists(email: String): Boolean =
        dsl.fetchCount(
            LOOKUP_EMAIL,
            LOOKUP_EMAIL.EMAIL.eq(email),
        ) > 0

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
