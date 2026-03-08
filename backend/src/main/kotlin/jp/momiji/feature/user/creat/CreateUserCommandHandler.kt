package jp.momiji.feature.user.creat

import de.huxhorn.sulky.ulid.ULID
import iss.jooq.generated.tables.LookupExternalIdentities.Companion.LOOKUP_EXTERNAL_IDENTITIES
import iss.jooq.generated.tables.references.LOOKUP_EMAIL
import jp.momiji.events.user.ExternalIdentityLinkedEvent
import jp.momiji.events.user.UserCreatedEvent
import jp.momiji.feature.CommandResult
import org.axonframework.messaging.commandhandling.annotation.CommandHandler
import org.axonframework.messaging.eventhandling.gateway.EventAppender
import org.jooq.DSLContext
import org.springframework.stereotype.Component

@Component
class CreateUserCommandHandler(
  private val dsl: DSLContext
) {
  @CommandHandler
  fun handle(
    command: CreateUserCommand,
    eventAppender: EventAppender
  ): CommandResult {
    // ①すでに issuer + subject が登録されていたら、何もしない(冪等性)
    if (existsByIssuerAndSubject(command.oidcIssuer, command.oidcSubject)) {
      return CreateUserCommandResult.success()
    }

    // ②Emailが検証されていなければエラーとする。
    // ①よりも後で行う理由は、一度外部IDPとリンクが終わった後に外部IDP側で、email_verifiedっがfalseになったとしても冪等性が機能するようにするため。
    if(!command.emailVerified) {
      return CreateUserCommandResult.emailNotVerified()
    }

    // ③すでにemailアドレスが登録されていたら、既存ユーザーとIDPのIDをリンクする
    val existingUserId = findUserIdByEmail(command.email)
    if (existingUserId != null) {
      eventAppender.append(
        ExternalIdentityLinkedEvent(
          userId = existingUserId,
          oidcIssuer = command.oidcIssuer,
          oidcSubject = command.oidcSubject
        )
      )
      return CreateUserCommandResult.success()
    }

    // ④新規ユーザー登録だった場合は、"ユーザー作成" "IDリンク"の２個イベントを出す
    val newUserId = ULID().nextULID()
    eventAppender.append(
      UserCreatedEvent(
        id = newUserId,
        email = command.email
      ),
      ExternalIdentityLinkedEvent(
        userId = newUserId,
        oidcIssuer = command.oidcIssuer,
        oidcSubject = command.oidcSubject,
      )
    )
    return CreateUserCommandResult.success()
  }

  private fun existsByIssuerAndSubject(issuer: String, subject: String): Boolean {
    return dsl.fetchCount(
      LOOKUP_EXTERNAL_IDENTITIES,
      LOOKUP_EXTERNAL_IDENTITIES.OIDC_ISSUER.eq(issuer)
        .and(LOOKUP_EXTERNAL_IDENTITIES.OIDC_SUBJECT.eq(subject))
    ) > 0
  }

  private fun findUserIdByEmail(email: String): String? {
    return dsl.select(LOOKUP_EMAIL.USER_ID)
      .from(LOOKUP_EMAIL)
      .where(LOOKUP_EMAIL.EMAIL.eq(email))
      .fetchOne(LOOKUP_EMAIL.USER_ID)
  }
}