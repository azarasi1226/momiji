package iss.application

import iss.jooq.generated.tables.references.EXTERNAL_IDENTITIES
import org.jooq.DSLContext
import org.springframework.stereotype.Component

@Component
class ExternalIdentityResolver(
  private val dsl: DSLContext
) {
  fun resolveUserId(oidcIssuer: String, oidcSubject: String): String? {
    return dsl.select(EXTERNAL_IDENTITIES.USERID)
      .from(EXTERNAL_IDENTITIES)
      .where(
        EXTERNAL_IDENTITIES.OIDC_ISSUER.eq(oidcIssuer)
          .and(EXTERNAL_IDENTITIES.OIDC_SUBJECT.eq(oidcSubject))
      )
      .fetchOne(EXTERNAL_IDENTITIES.USERID)
  }
}
