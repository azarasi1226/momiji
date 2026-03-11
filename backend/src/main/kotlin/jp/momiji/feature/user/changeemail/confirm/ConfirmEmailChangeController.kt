package jp.momiji.feature.user.changeemail.confirm

import jp.momiji.feature.throwIfError
import jp.momiji.feature.user.UserIdResolver
import org.axonframework.messaging.commandhandling.gateway.CommandGateway
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

data class ConfirmEmailChangeBody(val token: String)

@RestController
class ConfirmEmailChangeController(
  private val commandGateway: CommandGateway,
  private val userIdResolver: UserIdResolver,
) {
  @PostMapping("/users/me/email/change-confirm")
  fun confirmEmailChange(
    @RequestBody body: ConfirmEmailChangeBody,
    authentication: JwtAuthenticationToken,
  ) {
    val userId = userIdResolver.resolve(authentication)

    commandGateway.confirmEmailChange(
      ConfirmEmailChangeCommand(
        userId = userId,
        token = body.token,
      )
    ).throwIfError()
  }
}
