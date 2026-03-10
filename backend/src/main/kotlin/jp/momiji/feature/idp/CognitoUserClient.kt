package jp.momiji.feature.idp

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminGetUserRequest
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminUpdateUserAttributesRequest
import software.amazon.awssdk.services.cognitoidentityprovider.model.AttributeType
import software.amazon.awssdk.services.cognitoidentityprovider.model.UserNotFoundException

private val logger = KotlinLogging.logger {}

//@Component
class CognitoUserClient(
  @Value("\${momiji.cognito.user-pool-id}") private val userPoolId: String,
  private val cognitoClient: CognitoIdentityProviderClient,
) : IdpUserClient {
  override fun updateEmail(oidcSubject: String, newEmail: String) {
    try {
      cognitoClient.adminUpdateUserAttributes(
        AdminUpdateUserAttributesRequest.builder()
          .userPoolId(userPoolId)
          .username(oidcSubject)
          .userAttributes(
            AttributeType.builder().name("email").value(newEmail).build(),
            AttributeType.builder().name("email_verified").value("true").build(),
          )
          .build()
      )
    } catch (e: UserNotFoundException) {
      logger.warn { "Cognitoユーザーが見つかりません: oidcSubject=$oidcSubject" }
      return
    }

    logger.info { "Cognitoユーザーのメールアドレスを更新しました: oidcSubject=$oidcSubject" }
  }

  override fun getIdentityProvider(accessToken: String): IdentityProvider {
    val claims = com.nimbusds.jwt.SignedJWT.parse(accessToken).jwtClaimsSet
    val sub = claims.subject

    val response = cognitoClient.adminGetUser(
      AdminGetUserRequest.builder()
        .userPoolId(userPoolId)
        .username(sub)
        .build()
    )

    val identities = response.userAttributes()
      .firstOrNull { it.name() == "identities" }
      ?.value()

    if (identities != null && identities.contains("Google")) {
      return IdentityProvider.GOOGLE
    }

    return IdentityProvider.LOCAL
  }
}
