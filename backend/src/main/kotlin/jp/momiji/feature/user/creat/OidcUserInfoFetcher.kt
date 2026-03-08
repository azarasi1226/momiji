package jp.momiji.feature.user.creat

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

data class OidcUserInfo(
  val issuer: String,
  val subject: String,
  val email: String,
  val emailVerified: Boolean,
)

@Component
class OidcUserInfoFetcher(
  @Value("\${spring.security.oauth2.resourceserver.jwt.issuer-uri}") private val issuerUri: String,
) {
  private val restClient = RestClient.create()
  private val userinfoEndpoint: String

  init {
    val discovery = restClient.get()
      .uri("${issuerUri.trimEnd('/')}/.well-known/openid-configuration")
      .retrieve()
      .body(Map::class.java)
      ?: throw RuntimeException("Failed to fetch OIDC discovery document")

    userinfoEndpoint = discovery["userinfo_endpoint"] as String
  }

  fun handle(accessToken: String): OidcUserInfo {
    val response = restClient.get()
      .uri(userinfoEndpoint)
      .header("Authorization", "Bearer $accessToken")
      .retrieve()
      .body(Map::class.java)
      ?: throw RuntimeException("Failed to fetch userinfo")

    return OidcUserInfo(
      issuer = issuerUri,
      subject = response["sub"] as String,
      email = response["email"] as String,
      emailVerified = response["email_verified"] as? Boolean ?: false,
    )
  }
}
