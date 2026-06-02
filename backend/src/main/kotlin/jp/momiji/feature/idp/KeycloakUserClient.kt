package jp.momiji.feature.idp

import io.github.oshai.kotlinlogging.KotlinLogging
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
    private val keycloakAdminClient: KeycloakAdminClient,
) : IdpUserClient {
    private val restClient = RestClient.create()

    override fun updateEmail(
        oidcSubject: String,
        newEmail: String,
    ) {
        val token = keycloakAdminClient.getAdminToken()

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
            // ユーザーすでに削除されているということは２回叩かれた可能性が高いので冪等性を保つために例外は握りつぶし、ログだけ出す
            logger.error { "Keycloakユーザーが見つかりません: oidcSubject=$oidcSubject" }
            return
        }

        logger.info { "Keycloakユーザーのメールアドレスを更新しました: oidcSubject=$oidcSubject" }
    }

    override fun deleteUser(oidcSubject: String) {
        val token = keycloakAdminClient.getAdminToken()

        try {
            restClient
                .delete()
                .uri("$baseUrl/admin/realms/$realm/users/$oidcSubject")
                .header("Authorization", "Bearer $token")
                .retrieve()
                .toBodilessEntity()
        } catch (e: HttpClientErrorException.NotFound) {
            // ユーザーすでに削除されているということは２回叩かれた可能性が高いので冪等性を保つために例外は握りつぶし、ログだけ出す
            logger.error { "Keycloakユーザーが見つかりません: oidcSubject=$oidcSubject" }
            return
        }

        logger.info { "Keycloakユーザーを削除しました: oidcSubject=$oidcSubject" }
    }
}
