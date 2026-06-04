package jp.momiji.adapter.idp

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

/**
 * Keycloak Admin REST を叩くための master realm admin token を取得する共通クライアント。
 * [KeycloakUserClient] ( email 更新 / 削除 ) と [KeycloakUserInfoFetcher] ( federated-identity 取得 ) の
 * 両方が admin token を必要とするため、 token 取得 ( と admin 資格情報の保持 ) をここに集約する。
 */
@Component
@Profile("idp-keycloak")
class KeycloakAdminClient(
    @Value("\${momiji.keycloak.base-url}") private val baseUrl: String,
    @Value("\${momiji.keycloak.admin-username}") private val adminUsername: String,
    @Value("\${momiji.keycloak.admin-password}") private val adminPassword: String,
) {
    private val restClient = RestClient.create()

    fun getAdminToken(): String {
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
