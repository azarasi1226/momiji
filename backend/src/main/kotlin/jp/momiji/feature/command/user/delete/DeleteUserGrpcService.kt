package jp.momiji.feature.command.user.delete

import jp.momiji.config.grpc.GrpcAuthContext
import jp.momiji.feature.command.UserIdResolver
import jp.momiji.feature.command.throwIfError
import jp.momiji.grpc.momiji.user.delete.v1.DeleteUserRequest
import jp.momiji.grpc.momiji.user.delete.v1.DeleteUserResponse
import jp.momiji.grpc.momiji.user.delete.v1.DeleteUserServiceGrpcKt
import org.axonframework.messaging.commandhandling.gateway.CommandGateway
import org.springframework.stereotype.Service

@Service
class DeleteUserGrpcService(
    private val commandGateway: CommandGateway,
    private val userIdResolver: UserIdResolver,
) : DeleteUserServiceGrpcKt.DeleteUserServiceCoroutineImplBase() {
    override suspend fun deleteUser(request: DeleteUserRequest): DeleteUserResponse {
        val accessToken = GrpcAuthContext.current().token
        val userId = userIdResolver.resolve(accessToken)

        commandGateway
            .deleteUser(
                DeleteUserCommand(id = userId),
            ).throwIfError()

        return DeleteUserResponse.getDefaultInstance()
    }
}
