package iss.application

import iss.jooq.generated.tables.references.EXTERNAL_IDENTITIES
import iss.jooq.generated.tables.references.USERS
import org.jooq.DSLContext
import java.time.LocalDateTime
import java.util.UUID

data class CreateUserInput(
  val oidcIssuer: String,
  val oidcSubject: String,
  val email: String,
  val emailVerified: Boolean,
)

data class CreateUserOutput(val userId: String)

@org.springframework.stereotype.Component
class CreateUser(
  private val dsl: DSLContext,
  private val externalIdentityResolver: ExternalIdentityResolver,
) {
  fun handle(input: CreateUserInput): CreateUserOutput {
    return dsl.transactionResult { config ->
      val tx = config.dsl()

      // 1. 同じ外部IDで既に登録済みなら早期リターン（冪等性）
      val existingUserId = externalIdentityResolver.resolveUserId(input.oidcIssuer, input.oidcSubject)
      if (existingUserId != null) {
        return@transactionResult CreateUserOutput(userId = existingUserId)
      }

      // 2. email未検証なら拒否
      require(input.emailVerified) { "email is not verified" }

      // 3. 同一emailのユーザーが存在すれば紐づけ、なければ新規作成
      val existingUser = tx.select(USERS.ID)
        .from(USERS)
        .where(USERS.EMAIL.eq(input.email))
        .fetchOne()

      val userId = existingUser?.get(USERS.ID) ?: createNewUser(tx, input)

      // 4. external_identitiesに紐づけを作成
      tx.insertInto(EXTERNAL_IDENTITIES)
        .set(EXTERNAL_IDENTITIES.OIDC_ISSUER, input.oidcIssuer)
        .set(EXTERNAL_IDENTITIES.OIDC_SUBJECT, input.oidcSubject)
        .set(EXTERNAL_IDENTITIES.USERID, userId)
        .execute()

      CreateUserOutput(userId = userId)
    }
  }

  private fun createNewUser(tx: DSLContext, input: CreateUserInput): String {
    val userId = UUID.randomUUID().toString()
    val now = LocalDateTime.now()

    tx.insertInto(USERS)
      .set(USERS.ID, userId)
      .set(USERS.EMAIL, input.email)
      .set(USERS.NAME, "")
      .set(USERS.CREATED_AT, now)
      .set(USERS.UPDATED_AT, now)
      .execute()

    return userId
  }
}