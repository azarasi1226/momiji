package jp.momiji.feature.user.create

import com.github.michaelbull.result.getOrElse
import jp.momiji.config.grpc.GrpcAuthContext
import jp.momiji.domain.ValidationException
import jp.momiji.domain.user.Email
import jp.momiji.feature.idp.IdpUserClient
import jp.momiji.feature.throwIfError
import jp.momiji.grpc.momiji.user.create.v1.CreateUserRequest
import jp.momiji.grpc.momiji.user.create.v1.CreateUserResponse
import jp.momiji.grpc.momiji.user.create.v1.CreateUserServiceGrpcKt
import org.axonframework.messaging.commandhandling.gateway.CommandGateway
import org.springframework.stereotype.Service

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

        // userInfo.email は IDP 経由 (信頼境界内) だが、 broken IDP に対する防御で形式検証は通す。
        val email =
            Email.create(userInfo.email).getOrElse {
                throw ValidationException(listOf(it))
            }

        commandGateway
            .createUser(
                CreateUserCommand(
                    oidcSubject = userInfo.subject,
                    oidcIssuer = userInfo.issuer,
                    oidcIdentityProvider = idp.toString(),
                    email = email,
                    emailVerified = userInfo.emailVerified,
                ),
            ).throwIfError()

        return CreateUserResponse.getDefaultInstance()
    }
}
