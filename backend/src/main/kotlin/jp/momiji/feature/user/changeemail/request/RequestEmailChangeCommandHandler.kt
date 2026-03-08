package jp.momiji.feature.user.changeemail.request

import iss.jooq.generated.tables.references.LOOKUP_EMAIL
import jp.momiji.events.user.EmailChangeRequested
import jp.momiji.feature.CommandResult
import org.axonframework.messaging.commandhandling.annotation.CommandHandler
import org.axonframework.messaging.eventhandling.gateway.EventAppender
import org.jooq.DSLContext
import org.springframework.stereotype.Component

@Component
class RequestEmailChangeCommandHandler(
  private val dsl: DSLContext,
) {
  @CommandHandler
  fun handle(
    command: RequestEmailChangeCommand,
    eventAppender: EventAppender,
  ): CommandResult {
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
}
