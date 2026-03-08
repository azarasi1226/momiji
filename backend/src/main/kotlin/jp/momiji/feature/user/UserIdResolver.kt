package jp.momiji.feature.user

import iss.jooq.generated.tables.references.LOOKUP_EXTERNAL_IDENTITIES
import org.jooq.DSLContext
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.stereotype.Component

@Component
class UserIdResolver(
  private val dsl: DSLContext,
) {
  fun resolve(authentication: JwtAuthenticationToken): String? {
    val issuer = authentication.token.issuer.toString()
    val subject = authentication.token.subject

    return dsl.select(LOOKUP_EXTERNAL_IDENTITIES.USER_ID)
      .from(LOOKUP_EXTERNAL_IDENTITIES)
      .where(
        LOOKUP_EXTERNAL_IDENTITIES.OIDC_ISSUER.eq(issuer)
          .and(LOOKUP_EXTERNAL_IDENTITIES.OIDC_SUBJECT.eq(subject))
      )
      .fetchOne(LOOKUP_EXTERNAL_IDENTITIES.USER_ID)
  }
}
