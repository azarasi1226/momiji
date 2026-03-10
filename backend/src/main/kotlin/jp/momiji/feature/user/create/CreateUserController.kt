package jp.momiji.feature.user.create

import jp.momiji.feature.idp.IdpUserClient
import jp.momiji.feature.throwIfError
import org.axonframework.messaging.commandhandling.gateway.CommandGateway
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class CreateUserController(
  private val oidcUserInfoFetcher: OidcUserInfoFetcher,
  private val idpUserClient: IdpUserClient,
  private val commandGateway: CommandGateway,
) {

  @PostMapping("/users/me")
  fun createUser(authentication: JwtAuthenticationToken) {
    val accessToken = authentication.token.tokenValue
    val userInfo = oidcUserInfoFetcher.handle(accessToken)

    val idp = idpUserClient.getIdentityProvider(accessToken)

    commandGateway.createUser(
      CreateUserCommand(
        oidcSubject = userInfo.subject,
        oidcIssuer = userInfo.issuer,
        oidcIdentityProvider = idp.toString(),
        email = userInfo.email,
        emailVerified = userInfo.emailVerified
      )
    ).throwIfError()
  }
}
