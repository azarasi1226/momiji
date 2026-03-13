package jp.momiji.feature.user.changeemail.confirm

import iss.jooq.generated.tables.references.LOOKUP_EMAIL
import jp.momiji.events.MomijiEventTag
import jp.momiji.events.user.EmailChangeConfirmedEvent
import jp.momiji.events.user.UserCreatedEvent
import jp.momiji.feature.CommandResult
import jp.momiji.feature.user.changeemail.EmailChangeTokenService
import org.axonframework.eventsourcing.annotation.EventSourcingHandler
import org.axonframework.eventsourcing.annotation.reflection.EntityCreator
import org.axonframework.extension.spring.stereotype.EventSourced
import org.axonframework.messaging.commandhandling.annotation.CommandHandler
import org.axonframework.messaging.eventhandling.gateway.EventAppender
import org.axonframework.modelling.annotation.InjectEntity
import org.jooq.DSLContext
import org.springframework.stereotype.Component

@Component
class ConfirmEmailChangeCommandHandler(
  private val dsl: DSLContext,
  private val emailChangeTokenService: EmailChangeTokenService,
) {
  @CommandHandler
  fun handle(
    command: ConfirmEmailChangeCommand,
    @InjectEntity state: State,
    eventAppender: EventAppender,
  ): CommandResult {
    if (!state.created) {
      return ConfirmEmailChangeCommandResult.userNotFound()
    }

    val payload = emailChangeTokenService.verify(command.token)
    if (payload == null) {
      return ConfirmEmailChangeCommandResult.invalidToken()
    }

    if (command.userId != payload.userId) {
      return ConfirmEmailChangeCommandResult.userMismatch()
    }

    if (emailAlreadyExists(payload.newEmail)) {
      return ConfirmEmailChangeCommandResult.emailAlreadyInUse()
    }

    eventAppender.append(
      EmailChangeConfirmedEvent(
        userId = payload.userId,
        email = payload.newEmail,
      )
    )
    return ConfirmEmailChangeCommandResult.success()
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
