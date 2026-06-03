package jp.momiji.infrastructure.idp

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import io.github.oshai.kotlinlogging.KotlinLogging
import jp.momiji.domain.idp.IdentityProvider
import jp.momiji.domain.idp.IdentityProviderResolver
import jp.momiji.feature.idp.IdpUserInfoFetcher
import jp.momiji.feature.idp.OidcUserInfo
import jp.momiji.feature.idp.resolveEmail
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestClient

private val logger = KotlinLogging.logger {}

/**
 * Keycloak 用 [IdpUserInfoFetcher]。
 *
 * subject ( = Keycloak user id ) をキーに Admin REST で user representation ( email / emailVerified ) と
 * federated-identity ( IDP ) を取得する。 admin token は 1 度取って 2 つの GET で使い回す。
 */
@Component
@Profile("idp-keycloak")
class KeycloakUserInfoFetcher(
    @Value("\${momiji.keycloak.base-url}") private val baseUrl: String,
    @Value("\${momiji.keycloak.realm}") private val realm: String,
    private val keycloakAdminClient: KeycloakAdminClient,
) : IdpUserInfoFetcher {
    private val restClient = RestClient.create()

    private val identityProviderResolver =
        object : IdentityProviderResolver() {
            override val googleProviderName = "google"
        }

    /**
     * Keycloak `UserRepresentation` の必要フィールドだけ抽出した DTO。
     * コレをやらないとJacksonが例外を投げて失敗する。
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    private data class KeycloakUser(
        val email: String?,
        val emailVerified: Boolean?,
    )

    /** Keycloak `FederatedIdentityRepresentation` の必要フィールドだけ抽出した DTO。 */
    private data class KeycloakFederatedIdentity(
        val identityProvider: String,
    )

    override fun handle(
        subject: String,
        issuer: String,
    ): OidcUserInfo {
        val token = keycloakAdminClient.getAdminToken()
        val user = fetchUser(subject, token)

        return OidcUserInfo(
            issuer = issuer,
            subject = subject,
            // emailが空だった時点で resolveEmail 内で例外になる。 broken IDP / 属性マッピング漏れに対する防御。
            email = resolveEmail(user.email.orEmpty()),
            // Keycloak は emailVerified を boolean で返す。 null の可能性もあるが、 null なら false とみなし先に進んでも弾かれるようにする。
            emailVerified = user.emailVerified ?: false,
            identityProvider = resolveIdentityProvider(subject, token),
        )
    }

    /** Admin REST の user representation を取得する。 */
    private fun fetchUser(
        userId: String,
        token: String,
    ): KeycloakUser =
        restClient
            .get()
            .uri("$baseUrl/admin/realms/$realm/users/$userId")
            .header("Authorization", "Bearer $token")
            .retrieve()
            .body(KeycloakUser::class.java)
            ?: throw RuntimeException("Keycloak user representation の取得に失敗しました: userId=$userId")

    /**
     * federated-identity から IDP を解決する。 外部 IDP に link されていない ( 内蔵 ) user は空配列 → LOCAL。
     * momiji は IDP linking を使わないため配列は最大 1 要素。
     */
    private fun resolveIdentityProvider(
        userId: String,
        token: String,
    ): IdentityProvider {
        // federated-identity が空配列のときは ソーシャルIDPの紐づけがなされてないため ローカルユーザーとみなす。
        val federatedIdentity = fetchFederatedIdentities(userId, token).firstOrNull() ?: return IdentityProvider.LOCAL

        // federated-identity の provider をもとに IDP をホワイトリストにかける。
        return identityProviderResolver.resolve(federatedIdentity.identityProvider)
    }

    private fun fetchFederatedIdentities(
        userId: String,
        token: String,
    ): List<KeycloakFederatedIdentity> {
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
}
