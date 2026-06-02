package jp.momiji.feature.user.create

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
    @Value("\${momiji.oidc.issuer-uri}") private val issuerUri: String,
) {
    private val restClient = RestClient.create()
    private val userinfoEndpoint: String

    init {
        val discovery =
            restClient
                .get()
                .uri("${issuerUri.trimEnd('/')}/.well-known/openid-configuration")
                .retrieve()
                .body(Map::class.java)
                ?: throw RuntimeException("Failed to fetch OIDC discovery document")

        userinfoEndpoint = discovery["userinfo_endpoint"] as String
    }

    fun handle(accessToken: String): OidcUserInfo {
        val response =
            restClient
                .get()
                .uri(userinfoEndpoint)
                .header("Authorization", "Bearer $accessToken")
                .retrieve()
                .body(Map::class.java)
                ?: throw RuntimeException("Failed to fetch userinfo")

        return OidcUserInfo(
            issuer = issuerUri,
            subject = response["sub"] as String,
            email = response["email"] as String,
            // Cognito は email_verified を文字列 "true"/"false" で返す ( AWS 公式 doc 記載の仕様 ) が、
            // Keycloak は email_verified　をboolean で返す。 どちらでも拾えるよう toString() を挟んで判定する。
            // TODO: もはやOidcUserInfoFetcherも Interfaceにして KeycloakUserInfoFetcher と CognitoUserInfoFetcher に分けるべきかもしれない
            // そうすればresolveIdentityProviderも不要になるし、コードもシンプルになりそう
            // それかemail_verified対応をもう辞めるか。　　IPDのホワイトリストだけで十分な気もするのである。
            emailVerified = response["email_verified"]?.toString()?.toBooleanStrictOrNull() ?: false,
        )
    }
}
