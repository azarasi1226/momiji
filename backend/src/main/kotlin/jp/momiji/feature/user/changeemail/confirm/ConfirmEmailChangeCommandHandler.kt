package jp.momiji.feature.user.changeemail.confirm

import iss.jooq.generated.tables.references.LOOKUP_EMAIL
import iss.jooq.generated.tables.references.LOOKUP_EXTERNAL_IDENTITIES
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

    val loggedInUserId = resolveUserId(command.oidcIssuer, command.oidcSubject)
    if (loggedInUserId != payload.userId) {
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

  private fun resolveUserId(oidcIssuer: String, oidcSubject: String): String? {
    return dsl.select(LOOKUP_EXTERNAL_IDENTITIES.USER_ID)
      .from(LOOKUP_EXTERNAL_IDENTITIES)
      .where(
        LOOKUP_EXTERNAL_IDENTITIES.OIDC_ISSUER.eq(oidcIssuer)
          .and(LOOKUP_EXTERNAL_IDENTITIES.OIDC_SUBJECT.eq(oidcSubject))
      )
      .fetchOne(LOOKUP_EXTERNAL_IDENTITIES.USER_ID)
  }

  private fun emailAlreadyExists(email: String): Boolean {
    return dsl.fetchCount(
      LOOKUP_EMAIL,
      LOOKUP_EMAIL.EMAIL.eq(email)
    ) > 0
  }
}
