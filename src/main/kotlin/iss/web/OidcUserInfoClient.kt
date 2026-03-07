package iss.web

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

data class OidcUserInfo(
  val sub: String,
  val email: String?,
  val emailVerified: Boolean,
)

@Component
class OidcUserInfoClient(
  @Value("\${spring.security.oauth2.resourceserver.jwt.issuer-uri}") issuerUri: String,
) {
  private val restClient = RestClient.builder()
    .baseUrl(issuerUri.trimEnd('/'))
    .build()

  fun fetchUserInfo(accessToken: String): OidcUserInfo {
    val response = restClient.get()
      .uri("/userinfo")
      .header("Authorization", "Bearer $accessToken")
      .retrieve()
      .body(Map::class.java)
      ?: throw RuntimeException("Failed to fetch userinfo")

    return OidcUserInfo(
      sub = response["sub"] as String,
      email = response["email"] as? String,
      emailVerified = response["email_verified"] as? Boolean ?: false,
    )
  }
}
