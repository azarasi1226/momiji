package jp.momiji.feature.user.delete

import jp.momiji.config.grpc.GrpcAuthContext
import jp.momiji.feature.throwIfError
import jp.momiji.feature.user.UserIdResolver
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
