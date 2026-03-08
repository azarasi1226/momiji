package jp.momiji.feature.user.changeemail.confirm

import jp.momiji.feature.throwIfError
import org.axonframework.messaging.commandhandling.gateway.CommandGateway
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class ConfirmEmailChangeController(
  private val commandGateway: CommandGateway,
) {
  @GetMapping("/users/me/email/change-confirm")
  fun confirmEmailChange(
    @RequestParam token: String,
    authentication: JwtAuthenticationToken,
  ) {
    commandGateway.confirmEmailChange(
      ConfirmEmailChangeCommand(
        token = token,
        oidcIssuer = authentication.token.getClaimAsString("iss"),
        oidcSubject = authentication.token.getClaimAsString("sub"),
      )
    ).throwIfError()
  }
}
