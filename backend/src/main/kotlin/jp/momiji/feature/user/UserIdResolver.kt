package jp.momiji.feature.user

import io.github.oshai.kotlinlogging.KotlinLogging
import iss.jooq.generated.tables.references.LOOKUP_EXTERNAL_IDENTITIES
import jp.momiji.domain.BusinessError
import jp.momiji.domain.BusinessException
import org.jooq.DSLContext
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.stereotype.Component
import kotlin.requireNotNull

private val logger = KotlinLogging.logger {}

@Component
class UserIdResolver(
    private val dsl: DSLContext,
) {
    /**
     * AccessTokenからユーザーIDを解決する。
     */
    fun resolve(accessToken: Jwt): String =
        findUserId(accessToken)
            ?: throw BusinessException(BusinessError("ユーザーが登録されていません"))

    private fun findUserId(accessToken: Jwt): String? {
        val issuer = requireNotNull(accessToken.issuer) { "access token に iss claim がありません" }.toString()
        val subject = requireNotNull(accessToken.subject) { "access token に sub claim がありません" }

        return dsl
            .select(LOOKUP_EXTERNAL_IDENTITIES.USER_ID)
            .from(LOOKUP_EXTERNAL_IDENTITIES)
            .where(
                LOOKUP_EXTERNAL_IDENTITIES.OIDC_ISSUER
                    .eq(issuer)
                    .and(LOOKUP_EXTERNAL_IDENTITIES.OIDC_SUBJECT.eq(subject)),
            ).fetchOne(LOOKUP_EXTERNAL_IDENTITIES.USER_ID)
    }
}
