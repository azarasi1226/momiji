package jp.momiji.infrastructure.idp

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import jp.momiji.domain.idp.IdentityProvider
import jp.momiji.domain.idp.IdentityProviderResolver
import jp.momiji.feature.idp.IdpUserInfoFetcher
import jp.momiji.feature.idp.OidcUserInfo
import jp.momiji.feature.idp.resolveEmail
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminGetUserRequest

@Component
@Profile("idp-cognito")
class CognitoUserInfoFetcher(
    @Value("\${momiji.cognito.user-pool-id}") private val userPoolId: String,
    private val cognitoClient: CognitoIdentityProviderClient,
) : IdpUserInfoFetcher {
    private val identityProviderResolver =
        object : IdentityProviderResolver() {
            override val googleProviderName = "Google"
        }

    private companion object {
        private val objectMapper = jacksonObjectMapper()
    }

    /**
     * Cognito `identities` 要素の 'providerType' フィールドだけ抽出する DTO。
     * providerType は Cognito 固定値 ( Google / Facebook / SAML / OIDC 等 ) なので whitelist 判定に使う。
     *
     * dateCreated / userId / providerName / issuer / primary 等の無駄なフィールドも入ってるので ignoreUnknown = true で無視する。
     * コレをやらないとJacksonが例外を投げて失敗する。
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    private data class CognitoIdentity(
        val providerType: String,
    )

    override fun handle(
        subject: String,
        issuer: String,
    ): OidcUserInfo {
        val attributes = fetchUserAttributes(subject)

        return OidcUserInfo(
            issuer = issuer,
            subject = subject,
            email = resolveEmail(attributes["email"].orEmpty()),
            // Cognito は email_verified を文字列 "true"/"false" で返す ( AWS 公式 doc 記載の仕様 )。
            emailVerified = attributes["email_verified"]?.toBooleanStrictOrNull() ?: false,
            // もし Google / Facebook / SAML / OIDC などの IDP とリンクされていれば identities 属性に JSON 配列文字列で入る。
            // なぜ配列化というと、Cognitoには複数のIDPを1ユーザーにリンクできる機能があるため。今回はその機能は使わないため１要素しか入らない前提で実装する。
            identityProvider = resolveIdentityProvider(attributes["identities"]),
        )
    }

    private fun fetchUserAttributes(username: String): Map<String, String> =
        cognitoClient
            .adminGetUser(
                AdminGetUserRequest
                    .builder()
                    .userPoolId(userPoolId)
                    .username(username)
                    .build(),
            ).userAttributes()
            .associate { it.name() to it.value() }

    private fun resolveIdentityProvider(identitiesAttr: String?): IdentityProvider {
        // 空文字や null なら IDP とリンクされていないものとし、ローカルユーザーとみなす。
        if (identitiesAttr.isNullOrBlank()) return IdentityProvider.LOCAL

        // からの配列は想定されておらずありえないので UnknownError とする。 broken IDP の可能性がある。
        val identity =
            parseIdentities(identitiesAttr).firstOrNull()
                ?: throw IllegalStateException("Cognito identities が空配列で IDP を判定できません")

        return identityProviderResolver.resolve(identity.providerType)
    }

    private fun parseIdentities(identities: String): List<CognitoIdentity> =
        try {
            objectMapper.readValue<List<CognitoIdentity>>(identities)
        } catch (e: Exception) {
            // 想定外の形式 = 異常なので UnknownError として扱う。 broken IDP の可能性がある。
            throw IllegalStateException("Cognito identities の parse に失敗: identities=$identities", e)
        }
}
