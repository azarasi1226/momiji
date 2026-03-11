package jp.momiji.feature.user.update

import jp.momiji.events.MomijiEventTag
import jp.momiji.events.user.UserCreatedEvent
import jp.momiji.events.user.UserUpdatedEvent
import jp.momiji.feature.CommandResult
import org.axonframework.eventsourcing.annotation.EventSourcingHandler
import org.axonframework.eventsourcing.annotation.reflection.EntityCreator
import org.axonframework.extension.spring.stereotype.EventSourced
import org.axonframework.messaging.commandhandling.annotation.CommandHandler
import org.axonframework.messaging.eventhandling.gateway.EventAppender
import org.axonframework.modelling.annotation.InjectEntity
import org.springframework.stereotype.Component

@Component
class UpdateCommandHandler {
  @CommandHandler
  fun handle(
    command: UpdateUserCommand,
    @InjectEntity state: State,
    eventAppender: EventAppender
  ): CommandResult {
    if(!state.created) {
       return UpdateUserCommandResult.userNotFound()
    }

    eventAppender.append(
      UserUpdatedEvent(
        id = command.id,
        name = command.name,
        phoneNumber = command.phoneNumber,
        postalCode = command.postalCode,
        address1 = command.address1,
        address2 = command.address2
      )
    )
    return UpdateUserCommandResult.success()
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
