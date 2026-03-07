package iss.web

import iss.application.CreateUser
import iss.application.CreateUserInput
import iss.application.CreateUserOutput
import iss.application.GetUserInfo
import iss.application.GetUserInfoInput
import iss.application.UserInfo
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

@RestController
@RequestMapping("/users")
class UserController(
  private val createUser: CreateUser,
  private val getUserInfo: GetUserInfo,
  private val oidcUserInfoClient: OidcUserInfoClient,
) {

  @PostMapping("/me")
  fun createMe(@AuthenticationPrincipal jwt: Jwt): CreateUserOutput {
    // Access Token から iss/sub を取得
    val issuer = jwt.getClaimAsString("iss")
    val subject = jwt.getClaimAsString("sub")

    // email/email_verified は Access Token に含まれないため、 /userinfo を呼ぶ
    val userInfo = oidcUserInfoClient.fetchUserInfo(jwt.tokenValue)

    val input = CreateUserInput(
      oidcIssuer = issuer,
      oidcSubject = subject,
      email = userInfo.email
        ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "email is required"),
      emailVerified = userInfo.emailVerified,
    )
    try {
      return createUser.handle(input)
    } catch (e: IllegalArgumentException) {
      throw ResponseStatusException(HttpStatus.BAD_REQUEST, e.message)
    }
  }

  @GetMapping("/me")
  fun getMe(@AuthenticationPrincipal jwt: Jwt): UserInfo {
    // GetUserInfo は iss/sub のみで検索できるので /userinfo 呼び出し不要
    val input = GetUserInfoInput(
      oidcIssuer = jwt.getClaimAsString("iss"),
      oidcSubject = jwt.getClaimAsString("sub"),
    )
    return getUserInfo.handle(input)
      ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "User not found")
  }
}
