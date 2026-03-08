package jp.momiji.feature.user.changeemail.request

import jp.momiji.feature.throwIfError
import org.axonframework.messaging.commandhandling.gateway.CommandGateway
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

data class RequestEmailChangeBody(val newEmail: String)

@RestController
class RequestEmailChangeController(
  private val commandGateway: CommandGateway,
) {
  @PostMapping("/users/me/email/change-request")
  fun requestEmailChange(
    authentication: JwtAuthenticationToken,
    @RequestBody body: RequestEmailChangeBody,
  ) {
    val userId = authentication.token.subject

    commandGateway.requestEmailChange(
      RequestEmailChangeCommand(
        userId = userId,
        newEmail = body.newEmail,
      )
    ).throwIfError()
  }
}
