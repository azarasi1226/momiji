package jp.momiji.feature.user.update

import jp.momiji.feature.throwIfError
import jp.momiji.feature.user.UserIdResolver
import org.axonframework.messaging.commandhandling.gateway.CommandGateway
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

data class UpdateUserBody(
  val name: String,
  val phoneNumber: String,
  val postalCode: String,
  val address1: String,
  val address2: String
)

@RestController
class UpdateUserController(
  private val commandGateway: CommandGateway,
  private val userIdResolver: UserIdResolver,
) {
  @PutMapping("/users/me")
  fun updateUser(
    authentication: JwtAuthenticationToken,
    @RequestBody body: UpdateUserBody,
  ) {
    val userId = userIdResolver.resolve(authentication)

    commandGateway.updateUser(
      UpdateUserCommand(
        id = userId,
        name = body.name,
        phoneNumber = body.phoneNumber,
        postalCode = body.postalCode,
        address1 = body.address1,
        address2 = body.address2
      )
    ).throwIfError()
  }
}
