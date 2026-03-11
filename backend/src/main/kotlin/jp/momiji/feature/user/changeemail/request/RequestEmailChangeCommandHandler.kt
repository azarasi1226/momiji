package jp.momiji.feature.user.changeemail.request

import iss.jooq.generated.tables.references.LOOKUP_EMAIL
import jp.momiji.events.MomijiEventTag
import jp.momiji.events.user.EmailChangeRequested
import jp.momiji.events.user.UserCreatedEvent
import jp.momiji.feature.CommandResult
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
    if (!state.created) {
      return RequestEmailChangeCommandResult.userNotFound()
    }

    if (emailAlreadyExists(command.newEmail)) {
      return RequestEmailChangeCommandResult.emailAlreadyInUse()
    }

    eventAppender.append(
      EmailChangeRequested(
        userId = command.userId,
        newEmail = command.newEmail,
      )
    )
    return RequestEmailChangeCommandResult.success()
  }

  private fun emailAlreadyExists(email: String): Boolean {
    return dsl.fetchCount(
      LOOKUP_EMAIL,
      LOOKUP_EMAIL.EMAIL.eq(email)
    ) > 0
  }

  @EventSourced(tagKey = MomijiEventTag.USER_ID, idType = String::class)
  class State(
    var created: Boolean,
  ) {
    @EntityCreator
    constructor() : this(
      created = false,
    )

    @EventSourcingHandler
    fun evolve(event: UserCreatedEvent) {
      created = true
    }
  }
}
