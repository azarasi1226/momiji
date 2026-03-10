package jp.momiji.feature.user.findbyid

import jp.momiji.feature.Error
import jp.momiji.feature.UseCaseException
import jp.momiji.feature.user.UserIdResolver
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class FindUserByIdController(
  private val userIdResolver: UserIdResolver,
  private val findUserByIdQueryService: FindUserByIdQueryService,
) {
  @GetMapping("/users/me")
  fun handle(authentication: JwtAuthenticationToken): UserView {
    val userId = userIdResolver.resolve(authentication)
      ?: throw UseCaseException(Error("ユーザーが見つかりません"))

    return findUserByIdQueryService.findById(userId)
      ?: throw UseCaseException(Error("ユーザーが見つかりません"))
  }
}
