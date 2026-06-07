package jp.momiji.feature.command.user.changeemail.request

import com.github.michaelbull.result.getOrElse
import jp.momiji.config.grpc.GrpcAuthContext
import jp.momiji.domain.ValidationException
import jp.momiji.domain.user.Email
import jp.momiji.feature.command.throwIfError
import jp.momiji.feature.command.UserIdResolver
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
        val accessToken = GrpcAuthContext.current().token
        val userId = userIdResolver.resolve(accessToken)

        val newEmail =
            Email.create(request.newEmail).getOrElse {
                throw ValidationException(listOf(it))
            }

        commandGateway
            .requestEmailChange(
                RequestEmailChangeCommand(
                    userId = userId,
                    newEmail = newEmail,
                ),
            ).throwIfError()

        return RequestEmailChangeResponse.getDefaultInstance()
    }
}
