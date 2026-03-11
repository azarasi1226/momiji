package jp.momiji.feature.user.delete

import iss.jooq.generated.tables.LookupExternalIdentities.Companion.LOOKUP_EXTERNAL_IDENTITIES
import jp.momiji.events.MomijiEventTag
import jp.momiji.events.user.UserCreatedEvent
import jp.momiji.events.user.UserDeletedEvent
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
class DeleteUserCommandHandler(
  private val dsl: DSLContext
) {
  @CommandHandler
  fun handle(
    command: DeleteUserCommand,
    @InjectEntity state: State,
    eventAppender: EventAppender
  ): CommandResult {
    if (!state.created) {
      return DeleteUserCommandResult.userNotFound()
    }

    if (state.deleted) {
      return DeleteUserCommandResult.success()
    }

    eventAppender.append(
      UserDeletedEvent(id = command.id, oidcSubjects = findOidcSubjects(command.id))
    )
    return DeleteUserCommandResult.success()
  }

  private fun findOidcSubjects(userId: String): List<String> {
    return dsl.select(LOOKUP_EXTERNAL_IDENTITIES.OIDC_SUBJECT)
      .from(LOOKUP_EXTERNAL_IDENTITIES)
      .where(LOOKUP_EXTERNAL_IDENTITIES.USER_ID.eq(userId))
      .fetch(LOOKUP_EXTERNAL_IDENTITIES.OIDC_SUBJECT)
      .filterNotNull()
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
