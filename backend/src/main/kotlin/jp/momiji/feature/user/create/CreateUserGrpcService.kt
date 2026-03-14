package jp.momiji.feature.user.create

import jp.momiji.feature.idp.IdpUserClient
import jp.momiji.feature.throwIfError
import jp.momiji.grpc.GrpcAuthContext
import jp.momiji.grpc.momiji.user.create.v1.CreateUserRequest
import jp.momiji.grpc.momiji.user.create.v1.CreateUserResponse
import jp.momiji.grpc.momiji.user.create.v1.CreateUserServiceGrpcKt
import org.springframework.stereotype.Service
import org.axonframework.messaging.commandhandling.gateway.CommandGateway

@Service
class CreateUserGrpcService(
  private val oidcUserInfoFetcher: OidcUserInfoFetcher,
  private val idpUserClient: IdpUserClient,
  private val commandGateway: CommandGateway,
) : CreateUserServiceGrpcKt.CreateUserServiceCoroutineImplBase() {

  override suspend fun createUser(request: CreateUserRequest): CreateUserResponse {
    val auth = GrpcAuthContext.current()
    val accessToken = auth.token.tokenValue
    val userInfo = oidcUserInfoFetcher.handle(accessToken)
    val idp = idpUserClient.getIdentityProvider(accessToken)

    commandGateway.createUser(
      CreateUserCommand(
        oidcSubject = userInfo.subject,
        oidcIssuer = userInfo.issuer,
        oidcIdentityProvider = idp.toString(),
        email = userInfo.email,
        emailVerified = userInfo.emailVerified,
      )
    ).throwIfError()

    return CreateUserResponse.getDefaultInstance()
  }
}
