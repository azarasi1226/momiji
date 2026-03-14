package jp.momiji.feature.user.delete

import jp.momiji.feature.throwIfError
import jp.momiji.feature.user.UserIdResolver
import jp.momiji.grpc.GrpcAuthContext
import jp.momiji.grpc.momiji.user.delete.v1.DeleteUserRequest
import jp.momiji.grpc.momiji.user.delete.v1.DeleteUserResponse
import jp.momiji.grpc.momiji.user.delete.v1.DeleteUserServiceGrpcKt
import org.springframework.stereotype.Service
import org.axonframework.messaging.commandhandling.gateway.CommandGateway

@Service
class DeleteUserGrpcService(
  private val commandGateway: CommandGateway,
  private val userIdResolver: UserIdResolver,
) : DeleteUserServiceGrpcKt.DeleteUserServiceCoroutineImplBase() {

  override suspend fun deleteUser(request: DeleteUserRequest): DeleteUserResponse {
    val auth = GrpcAuthContext.current()
    val userId = userIdResolver.resolve(auth)

    commandGateway.deleteUser(
      DeleteUserCommand(id = userId)
    ).throwIfError()

    return DeleteUserResponse.getDefaultInstance()
  }
}
