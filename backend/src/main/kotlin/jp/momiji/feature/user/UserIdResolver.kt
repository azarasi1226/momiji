package jp.momiji.feature.user

import io.github.oshai.kotlinlogging.KotlinLogging
import iss.jooq.generated.tables.references.LOOKUP_EXTERNAL_IDENTITIES
import jp.momiji.domain.BusinessError
import jp.momiji.domain.BusinessException
import org.jooq.DSLContext
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

@Component
class UserIdResolver(
    private val dsl: DSLContext,
) {
    /**
     * AccessTokenからユーザーIDを解決する。
     */
    fun resolve(authentication: JwtAuthenticationToken): String =
        findUserId(authentication)
            ?: throw BusinessException(BusinessError("ユーザーが登録されていません"))

    private fun findUserId(authentication: JwtAuthenticationToken): String? {
        val issuer = authentication.token.issuer.toString()
        val subject = authentication.token.subject

        // issuerと違いsubjectは AccessTokenに必ずとも含めなくても良いことになっているため、 Keycloakのような一部のIDPではSubjectクレームがAccessTokenに含まれないことがある。
        // しかし、AccessTokenから解決できるようにしないとUserInfoEndpointを毎回問い合わせることになりパフォーマンス的に良くない。
        // そのため、 対応していないIDPを使用している場合は、AccessTokenにSubjectクレームを含めるようIDP側の設定を変更することを要求する。
        if (subject == null) {
            logger.error { "アクセストークンにSubjectクレームが含まれていません。 IDP側の設定でAccessTokenにsubクレームを含むように設定してください。" }
            throw BusinessException(BusinessError("アクセストークンにSubjectクレームが含まれていません。管理者へ連絡してください。"))
        }

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
