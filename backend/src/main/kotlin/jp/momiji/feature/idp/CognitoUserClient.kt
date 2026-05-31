package jp.momiji.feature.idp

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.github.oshai.kotlinlogging.KotlinLogging
import jp.momiji.domain.BusinessError
import jp.momiji.domain.UseCaseException
import jp.momiji.domain.idp.IdentityProvider
import jp.momiji.domain.idp.IdentityProviderResolver
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminGetUserRequest
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminUpdateUserAttributesRequest
import software.amazon.awssdk.services.cognitoidentityprovider.model.AttributeType
import software.amazon.awssdk.services.cognitoidentityprovider.model.UserNotFoundException

private val logger = KotlinLogging.logger {}

@Component
@Profile("idp-cognito")
class CognitoUserClient(
    @Value("\${momiji.cognito.user-pool-id}") private val userPoolId: String,
    private val cognitoClient: CognitoIdentityProviderClient,
) : IdpUserClient {
    private val identityProviderResolver =
        object : IdentityProviderResolver() {
            override val googleProviderName = "Google"
        }

    private companion object {
        private val objectMapper = jacksonObjectMapper()
    }

    /**
     * Cognito `identities` attribute の各要素を表す DTO ( 必要なフィールドだけ抽出 )。
     *
     * `providerType` は Cognito 固定値 ( `Google` / `Facebook` / `SAML` / `OIDC` 等 ) なので
     * whitelist 判定に使う。 `providerName` はユーザーが Cognito 上で自由に付けた IDP 名で
     * 変更可能なため、 安定した識別子としては `providerType` を採用する。
     */
    private data class CognitoIdentity(
        val providerType: String,
    )

    override fun updateEmail(
        oidcSubject: String,
        newEmail: String,
    ) {
        try {
            cognitoClient.adminUpdateUserAttributes(
                AdminUpdateUserAttributesRequest
                    .builder()
                    .userPoolId(userPoolId)
                    .username(oidcSubject)
                    .userAttributes(
                        AttributeType
                            .builder()
                            .name("email")
                            .value(newEmail)
                            .build(),
                        AttributeType
                            .builder()
                            .name("email_verified")
                            .value("true")
                            .build(),
                    ).build(),
            )
        } catch (e: UserNotFoundException) {
            logger.error { "Cognitoユーザーが見つかりません: oidcSubject=$oidcSubject" }
            return
        }

        logger.info { "Cognitoユーザーのメールアドレスを更新しました: oidcSubject=$oidcSubject" }
    }

    override fun deleteUser(oidcSubject: String) {
        try {
            cognitoClient.adminDeleteUser(
                software.amazon.awssdk.services.cognitoidentityprovider.model.AdminDeleteUserRequest
                    .builder()
                    .userPoolId(userPoolId)
                    .username(oidcSubject)
                    .build(),
            )
        } catch (e: UserNotFoundException) {
            logger.error { "Cognitoユーザーが見つかりません: oidcSubject=$oidcSubject" }
            return
        }

        logger.info { "Cognitoユーザーを削除しました: oidcSubject=$oidcSubject" }
    }

    override fun resolveIdentityProvider(accessToken: String): IdentityProvider {
        val sub =
            com.nimbusds.jwt.SignedJWT
                .parse(accessToken)
                .jwtClaimsSet.subject

        // Cognito 内蔵 user は `identities` attribute 自体存在しない → null/空文字で LOCAL 扱い。
        val identitiesAttr = fetchIdentitiesAttribute(sub)
        if (identitiesAttr.isNullOrBlank()) return IdentityProvider.LOCAL

        // Cognito は federated user の初回 sign-in で新規 user profile を作るため ( デフォルト = auto-link しない )、
        // momiji の運用 ( `AdminLinkProviderForUser` での IDP linking を使わない ) では 1 user = 1 IDP =
        // identities 配列は 1 要素。 将来 linking を導入するなら配列順序の保証がないため要再設計。
        val identity =
            parseIdentities(identitiesAttr).firstOrNull()
                ?: return IdentityProvider.LOCAL

        // 未対応 providerType ( SAML や FACEBOOK 等 ) は resolver 内で fail-closed の UseCaseException が投げられる。
        return identityProviderResolver.resolve(identity.providerType)
    }

    /** Cognito の AdminGetUser API を叩いて、 `identities` user attribute の生文字列を返す。 無ければ null。 */
    private fun fetchIdentitiesAttribute(sub: String): String? {
        val response =
            cognitoClient.adminGetUser(
                AdminGetUserRequest
                    .builder()
                    .userPoolId(userPoolId)
                    .username(sub)
                    .build(),
            )
        return response
            .userAttributes()
            .firstOrNull { it.name() == "identities" }
            ?.value()
    }

    /**
     * 厳密に JSON parse して [CognitoIdentity] のリストにする。
     * 単純な `contains("Google")` 等の文字列マッチだと、 別フィールド (userId 等) に文字列が
     * 偶然含まれて false positive のリスクがあるため、 構造化 parse を必須としている。
     */
    private fun parseIdentities(identitiesAttribute: String): List<CognitoIdentity> =
        try {
            objectMapper.readValue<List<CognitoIdentity>>(identitiesAttribute)
        } catch (e: Exception) {
            logger.error(e) { "Cognito の identities attribute の parse に失敗: identities=$identitiesAttribute" }
            throw UseCaseException(BusinessError("ユーザーのIDP情報の取得に失敗しました"))
        }
}
