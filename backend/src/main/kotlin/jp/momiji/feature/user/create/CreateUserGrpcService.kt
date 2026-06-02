package jp.momiji.feature.user.create

import jp.momiji.config.grpc.GrpcAuthContext
import jp.momiji.feature.idp.IdpUserInfoFetcher
import jp.momiji.feature.throwIfError
import jp.momiji.grpc.momiji.user.create.v1.CreateUserRequest
import jp.momiji.grpc.momiji.user.create.v1.CreateUserResponse
import jp.momiji.grpc.momiji.user.create.v1.CreateUserServiceGrpcKt
import org.axonframework.messaging.commandhandling.gateway.CommandGateway
import org.springframework.stereotype.Service

@Service
class CreateUserGrpcService(
    private val idpUserInfoFetcher: IdpUserInfoFetcher,
    private val commandGateway: CommandGateway,
) : CreateUserServiceGrpcKt.CreateUserServiceCoroutineImplBase() {
    override suspend fun createUser(request: CreateUserRequest): CreateUserResponse {
        // access token は Spring Security で検証済み。 その subject / issuer を使って
        // IDP の admin API から「完成品」 の OidcUserInfo ( identityProvider / email 値オブジェクト込み ) を解決する。
        val token = GrpcAuthContext.current().token
        val userInfo =
            idpUserInfoFetcher.handle(
                subject = requireNotNull(token.subject) { "access token に sub claim がありません" },
                issuer = requireNotNull(token.issuer) { "access token に iss claim がありません" }.toString(),
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
