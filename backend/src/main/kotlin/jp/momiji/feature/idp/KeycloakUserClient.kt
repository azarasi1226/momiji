package jp.momiji.feature.idp

import io.github.oshai.kotlinlogging.KotlinLogging
import jp.momiji.domain.idp.IdentityProvider
import jp.momiji.domain.idp.IdentityProviderResolver
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestClient

private val logger = KotlinLogging.logger {}

@Component
@Profile("idp-keycloak")
class KeycloakUserClient(
    @Value("\${momiji.keycloak.base-url}") private val baseUrl: String,
    @Value("\${momiji.keycloak.realm}") private val realm: String,
    @Value("\${momiji.keycloak.admin-username}") private val adminUsername: String,
    @Value("\${momiji.keycloak.admin-password}") private val adminPassword: String,
) : IdpUserClient {
    private val restClient = RestClient.create()

    private val identityProviderResolver =
        object : IdentityProviderResolver() {
            override val googleProviderName = "google"
        }

    override fun updateEmail(
        oidcSubject: String,
        newEmail: String,
    ) {
        val token = getAdminToken()

        try {
            restClient
                .put()
                .uri("$baseUrl/admin/realms/$realm/users/$oidcSubject")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .body(mapOf("email" to newEmail, "emailVerified" to true))
                .retrieve()
                .toBodilessEntity()
        } catch (e: HttpClientErrorException.NotFound) {
            logger.error { "Keycloakユーザーが見つかりません: oidcSubject=$oidcSubject" }
            return
        }

        logger.info { "Keycloakユーザーのメールアドレスを更新しました: oidcSubject=$oidcSubject" }
    }

    override fun deleteUser(oidcSubject: String) {
        val token = getAdminToken()

        try {
            restClient
                .delete()
                .uri("$baseUrl/admin/realms/$realm/users/$oidcSubject")
                .header("Authorization", "Bearer $token")
                .retrieve()
                .toBodilessEntity()
        } catch (e: HttpClientErrorException.NotFound) {
            logger.error { "Keycloakユーザーが見つかりません: oidcSubject=$oidcSubject" }
            return
        }

        logger.info { "Keycloakユーザーを削除しました: oidcSubject=$oidcSubject" }
    }

    override fun resolveIdentityProvider(accessToken: String): IdentityProvider {
        // access token は auth proof として最低限の用途のみ使い、 IDP の判定は Keycloak Admin REST API で
        // user の federated-identity を直接問い合わせる ( Cognito の AdminGetUser + identities attribute と対称の設計 )。
        // realm.json で custom claim mapper を持たないため、 access token はピュアな OIDC 標準 claim のみ含む。
        val sub =
            com.nimbusds.jwt.SignedJWT
                .parse(accessToken)
                .jwtClaimsSet.subject

        // Keycloak Identity Brokering で外部 IDP に link されてない user ( = Keycloak 内蔵 user ) は
        // federated-identity が空配列。 social IDP 経由 sign-in した user は配列に該当 IDP alias の要素を持つ。
        // momiji は IDP linking を使わない運用 ( [ADR 0003](../../../../../../docs/adr/0003-idp-linking.md) ) なので、
        // 配列は最大 1 要素。 firstOrNull() で OK。
        val federatedIdentity =
            fetchFederatedIdentities(sub).firstOrNull()
                ?: return IdentityProvider.LOCAL

        return identityProviderResolver.resolve(federatedIdentity.identityProvider)
    }

    /** Keycloak `FederatedIdentityRepresentation` の必要フィールドだけ抽出した DTO。 */
    private data class KeycloakFederatedIdentity(
        val identityProvider: String,
    )

    /** Keycloak Admin REST API から user に link された federated identity の一覧を取得。 */
    private fun fetchFederatedIdentities(userId: String): List<KeycloakFederatedIdentity> {
        val token = getAdminToken()
        val response =
            try {
                restClient
                    .get()
                    .uri("$baseUrl/admin/realms/$realm/users/$userId/federated-identity")
                    .header("Authorization", "Bearer $token")
                    .retrieve()
                    .body(Array<KeycloakFederatedIdentity>::class.java)
            } catch (e: HttpClientErrorException.NotFound) {
                logger.error { "Keycloakユーザーが見つかりません: oidcSubject=$userId" }
                return emptyList()
            }
        return response?.toList() ?: emptyList()
    }

    private fun getAdminToken(): String {
        val response =
            restClient
                .post()
                .uri("$baseUrl/realms/master/protocol/openid-connect/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body("grant_type=password&client_id=admin-cli&username=$adminUsername&password=$adminPassword")
                .retrieve()
                .body(Map::class.java)
                ?: throw RuntimeException("Keycloakの管理トークンの取得に失敗しました")

        return response["access_token"] as String
    }
}
