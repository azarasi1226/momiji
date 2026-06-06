package jp.momiji.feature.command.user.create

import jp.momiji.config.grpc.GrpcAuthContext
import jp.momiji.feature.command.throwIfError
import jp.momiji.grpc.momiji.user.create.v1.CreateUserRequest
import jp.momiji.grpc.momiji.user.create.v1.CreateUserResponse
import jp.momiji.grpc.momiji.user.create.v1.CreateUserServiceGrpcKt
import jp.momiji.port.idp.IdpUserInfoFetcher
import org.axonframework.messaging.commandhandling.gateway.CommandGateway
import org.springframework.stereotype.Service

@Service
class CreateUserGrpcService(
    private val idpUserInfoFetcher: IdpUserInfoFetcher,
    private val commandGateway: CommandGateway,
) : CreateUserServiceGrpcKt.CreateUserServiceCoroutineImplBase() {
    override suspend fun createUser(request: CreateUserRequest): CreateUserResponse {
        val accessToken = GrpcAuthContext.current().token
        val userInfo =
            idpUserInfoFetcher.handle(
                subject = requireNotNull(accessToken.subject) { "access token に sub claim がありません" },
                issuer = requireNotNull(accessToken.issuer) { "access token に iss claim がありません" }.toString(),
            )

        commandGateway
            .createUser(
                CreateUserCommand(
                    oidcSubject = userInfo.subject,
                    oidcIssuer = userInfo.issuer,
                    oidcIdentityProvider = userInfo.identityProvider,
                    email = userInfo.email,
                    emailVerified = userInfo.emailVerified,
                ),
            ).throwIfError()

        return CreateUserResponse.getDefaultInstance()
    }
}
