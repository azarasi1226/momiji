package jp.momiji.feature.command.user.update

import com.github.michaelbull.result.onErr
import com.github.michaelbull.result.onOk
import jp.momiji.config.grpc.GrpcAuthContext
import jp.momiji.domain.ValidationException
import jp.momiji.domain.user.Name
import jp.momiji.feature.command.UserIdResolver
import jp.momiji.feature.command.throwIfError
import jp.momiji.grpc.momiji.user.update.UpdateUserRequest
import jp.momiji.grpc.momiji.user.update.UpdateUserResponse
import jp.momiji.grpc.momiji.user.update.UpdateUserServiceGrpcKt
import org.axonframework.messaging.commandhandling.gateway.CommandGateway
import org.springframework.stereotype.Service

@Service
class UpdateUserGrpcService(
    private val commandGateway: CommandGateway,
    private val userIdResolver: UserIdResolver,
) : UpdateUserServiceGrpcKt.UpdateUserServiceCoroutineImplBase() {
    override suspend fun updateUser(request: UpdateUserRequest): UpdateUserResponse {
        val accessToken = GrpcAuthContext.current().token
        val userId = userIdResolver.resolve(accessToken)

        Name
            .create(request.name)
            .onErr { error -> throw ValidationException(listOf(error)) }
            .onOk { name ->
                commandGateway
                    .updateUser(UpdateUserCommand(id = userId, name = name))
                    .throwIfError()
            }

        return UpdateUserResponse.getDefaultInstance()
    }
}
