package jp.momiji.feature.user.delete

import jp.momiji.feature.throwIfError
import jp.momiji.feature.user.UserIdResolver
import org.axonframework.messaging.commandhandling.gateway.CommandGateway
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class DeleteUserController(
  private val commandGateway: CommandGateway,
  private val userIdResolver: UserIdResolver,
) {
  @DeleteMapping("/users/me")
  fun deleteUser(
    authentication: JwtAuthenticationToken,
  ) {
    val userId = userIdResolver.resolve(authentication)

    commandGateway.deleteUser(
      DeleteUserCommand(id = userId)
    ).throwIfError()
  }
}
