package jp.momiji.feature.user.changeemail.request

import jp.momiji.feature.throwIfError
import jp.momiji.feature.user.UserIdResolver
import jp.momiji.config.grpc.GrpcAuthContext
import jp.momiji.grpc.momiji.user.changeemail.request.v1.RequestEmailChangeRequest
import jp.momiji.grpc.momiji.user.changeemail.request.v1.RequestEmailChangeResponse
import jp.momiji.grpc.momiji.user.changeemail.request.v1.RequestEmailChangeServiceGrpcKt
import org.axonframework.messaging.commandhandling.gateway.CommandGateway
import org.springframework.stereotype.Service

@Service
class RequestEmailChangeGrpcService(
    private val commandGateway: CommandGateway,
    private val userIdResolver: UserIdResolver,
) : RequestEmailChangeServiceGrpcKt.RequestEmailChangeServiceCoroutineImplBase() {
    override suspend fun requestEmailChange(request: RequestEmailChangeRequest): RequestEmailChangeResponse {
        val auth = GrpcAuthContext.current()
        val userId = userIdResolver.resolve(auth)

        commandGateway
            .requestEmailChange(
                RequestEmailChangeCommand(
                    userId = userId,
                    newEmail = request.newEmail,
                ),
            ).throwIfError()

        return RequestEmailChangeResponse.getDefaultInstance()
    }
}
