package jp.momiji.feature.command.user.changeemail.confirm

import com.github.michaelbull.result.getOrElse
import jp.momiji.config.grpc.GrpcAuthContext
import jp.momiji.domain.ValidationException
import jp.momiji.domain.user.EmailChangeToken
import jp.momiji.feature.command.throwIfError
import jp.momiji.feature.command.user.UserIdResolver
import jp.momiji.grpc.momiji.user.changeemail.confirm.v1.ConfirmEmailChangeRequest
import jp.momiji.grpc.momiji.user.changeemail.confirm.v1.ConfirmEmailChangeResponse
import jp.momiji.grpc.momiji.user.changeemail.confirm.v1.ConfirmEmailChangeServiceGrpcKt
import org.axonframework.messaging.commandhandling.gateway.CommandGateway
import org.springframework.stereotype.Service

@Service
class ConfirmEmailChangeGrpcService(
    private val commandGateway: CommandGateway,
    private val userIdResolver: UserIdResolver,
) : ConfirmEmailChangeServiceGrpcKt.ConfirmEmailChangeServiceCoroutineImplBase() {
    override suspend fun confirmEmailChange(request: ConfirmEmailChangeRequest): ConfirmEmailChangeResponse {
        val accessToken = GrpcAuthContext.current().token
        val userId = userIdResolver.resolve(accessToken)

        val token =
            EmailChangeToken.create(request.token).getOrElse {
                throw ValidationException(listOf(it))
            }

        commandGateway
            .confirmEmailChange(
                ConfirmEmailChangeCommand(
                    userId = userId,
                    token = token,
                ),
            ).throwIfError()

        return ConfirmEmailChangeResponse.getDefaultInstance()
    }
}
