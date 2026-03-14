package jp.momiji.feature.user.update

import jp.momiji.feature.throwIfError
import jp.momiji.feature.user.UserIdResolver
import jp.momiji.grpc.GrpcAuthContext
import jp.momiji.grpc.momiji.user.update.v1.UpdateUserRequest
import jp.momiji.grpc.momiji.user.update.v1.UpdateUserResponse
import jp.momiji.grpc.momiji.user.update.v1.UpdateUserServiceGrpcKt
import org.springframework.stereotype.Service
import org.axonframework.messaging.commandhandling.gateway.CommandGateway

@Service
class UpdateUserGrpcService(
  private val commandGateway: CommandGateway,
  private val userIdResolver: UserIdResolver,
) : UpdateUserServiceGrpcKt.UpdateUserServiceCoroutineImplBase() {

  override suspend fun updateUser(request: UpdateUserRequest): UpdateUserResponse {
    val auth = GrpcAuthContext.current()
    val userId = userIdResolver.resolve(auth)

    commandGateway.updateUser(
      UpdateUserCommand(
        id = userId,
        name = request.name,
        phoneNumber = request.phoneNumber,
        postalCode = request.postalCode,
        address1 = request.address1,
        address2 = request.address2,
      )
    ).throwIfError()

    return UpdateUserResponse.getDefaultInstance()
  }
}
