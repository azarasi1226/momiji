package jp.momiji.feature.user.creat

import org.axonframework.messaging.commandhandling.gateway.CommandGateway
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class CreateUserController(
  private val commandGateway: CommandGateway,
  private val oidcUserInfoFetcher: OidcUserInfoFetcher,
) {

  @PostMapping("/users/me")
  fun createUser(authentication: JwtAuthenticationToken) {
    val accessToken = authentication.token.tokenValue
    val userInfo = oidcUserInfoFetcher.handle(accessToken)

    commandGateway.sendAndWait(
      CreateUserCommand(
        oidcSubject = userInfo.subject,
        oidcIssuer = userInfo.issuer,
        email = userInfo.email,
        emailVerified = userInfo.emailVerified
      )
    )
  }
}