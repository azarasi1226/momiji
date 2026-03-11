package jp.momiji.feature.idp

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

private val logger = KotlinLogging.logger {}

@Component
class KeycloakUserClient(
  @Value("\${momiji.keycloak.base-url:http://localhost:8085}") private val baseUrl: String,
  @Value("\${momiji.keycloak.realm:momiji}") private val realm: String,
  @Value("\${momiji.keycloak.admin-username:admin}") private val adminUsername: String,
  @Value("\${momiji.keycloak.admin-password:admin}") private val adminPassword: String,
) : IdpUserClient {
  private val restClient = RestClient.create()

  override fun updateEmail(oidcSubject: String, newEmail: String) {
    val token = getAdminToken()

    restClient.put()
      .uri("$baseUrl/admin/realms/$realm/users/$oidcSubject")
      .header("Authorization", "Bearer $token")
      .contentType(MediaType.APPLICATION_JSON)
      .body(mapOf("email" to newEmail, "emailVerified" to true))
      .retrieve()
      .toBodilessEntity()

    logger.info { "Keycloakユーザーのメールアドレスを更新しました: oidcSubject=$oidcSubject" }
  }

  override fun deleteUser(oidcSubject: String) {
    val token = getAdminToken()

    restClient.delete()
      .uri("$baseUrl/admin/realms/$realm/users/$oidcSubject")
      .header("Authorization", "Bearer $token")
      .retrieve()
      .toBodilessEntity()

    logger.info { "Keycloakユーザーを削除しました: oidcSubject=$oidcSubject" }
  }

  override fun getIdentityProvider(accessToken: String): IdentityProvider {
    val claims = com.nimbusds.jwt.SignedJWT.parse(accessToken).jwtClaimsSet
    val idp = claims.getStringClaim("identity_provider") ?: return IdentityProvider.LOCAL

    return when (idp) {
      "google" -> IdentityProvider.GOOGLE
      else -> IdentityProvider.LOCAL
    }
  }

  private fun getAdminToken(): String {
    val response = restClient.post()
      .uri("$baseUrl/realms/master/protocol/openid-connect/token")
      .contentType(MediaType.APPLICATION_FORM_URLENCODED)
      .body("grant_type=password&client_id=admin-cli&username=$adminUsername&password=$adminPassword")
      .retrieve()
      .body(Map::class.java)
      ?: throw RuntimeException("Keycloakの管理トークンの取得に失敗しました")

    return response["access_token"] as String
  }
}
