package jp.momiji.feature.user.changeemail.confirm

import jp.momiji.feature.Error
import jp.momiji.feature.UseCaseException
import jp.momiji.feature.throwIfError
import jp.momiji.feature.user.UserIdResolver
import org.axonframework.messaging.commandhandling.gateway.CommandGateway
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class ConfirmEmailChangeController(
  private val commandGateway: CommandGateway,
  private val userIdResolver: UserIdResolver,
) {
  @GetMapping("/users/me/email/change-confirm")
  fun confirmEmailChange(
    @RequestParam token: String,
    authentication: JwtAuthenticationToken,
  ) {
    val userId = userIdResolver.resolve(authentication)
      ?: throw UseCaseException(Error("ユーザーが見つかりません"))
    commandGateway.confirmEmailChange(
      ConfirmEmailChangeCommand(
        userId = userId,
        token = token,
      )
    ).throwIfError()
  }
}
