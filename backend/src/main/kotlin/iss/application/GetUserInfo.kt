package iss.application

import iss.jooq.generated.tables.references.USERS
import org.jooq.DSLContext
import org.springframework.stereotype.Component

data class GetUserInfoInput(
  val oidcIssuer: String,
  val oidcSubject: String,
)

data class UserInfo(
  val userId: String,
  val name: String,
  val email: String,
)

@Component
class GetUserInfo(
  private val dsl: DSLContext,
  private val externalIdentityResolver: ExternalIdentityResolver,
) {
  fun handle(input: GetUserInfoInput): UserInfo? {
    val userId = externalIdentityResolver.resolveUserId(input.oidcIssuer, input.oidcSubject)
      ?: return null

    val record = dsl.select(USERS.NAME, USERS.EMAIL)
      .from(USERS)
      .where(USERS.ID.eq(userId))
      .fetchOne() ?: return null

    return UserInfo(
      userId = userId,
      name = record[USERS.NAME]!!,
      email = record[USERS.EMAIL]!!,
    )
  }
}