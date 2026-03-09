package jp.momiji.feature.user.changeemail.confirm

import iss.jooq.generated.tables.references.LOOKUP_EMAIL
import jp.momiji.events.user.EmailChangeConfirmed
import jp.momiji.feature.CommandResult
import jp.momiji.feature.user.changeemail.EmailChangeTokenService
import org.axonframework.messaging.commandhandling.annotation.CommandHandler
import org.axonframework.messaging.eventhandling.gateway.EventAppender
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
    eventAppender: EventAppender,
  ): CommandResult {
    val payload = emailChangeTokenService.verify(command.token)
    if(payload == null) {
      return ConfirmEmailChangeCommandResult.invalidToken()
    }

    if (command.userId != payload.userId) {
      return ConfirmEmailChangeCommandResult.userMismatch()
    }

    if (emailAlreadyExists(payload.newEmail)) {
      return ConfirmEmailChangeCommandResult.emailAlreadyInUse()
    }

    eventAppender.append(
      EmailChangeConfirmed(
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
}
