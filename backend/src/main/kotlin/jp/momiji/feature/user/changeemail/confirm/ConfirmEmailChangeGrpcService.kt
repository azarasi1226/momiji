package jp.momiji.feature.user.changeemail.confirm

import jp.momiji.feature.throwIfError
import jp.momiji.feature.user.UserIdResolver
import jp.momiji.grpc.GrpcAuthContext
import jp.momiji.grpc.momiji.user.changeemail.confirm.v1.ConfirmEmailChangeRequest
import jp.momiji.grpc.momiji.user.changeemail.confirm.v1.ConfirmEmailChangeResponse
import jp.momiji.grpc.momiji.user.changeemail.confirm.v1.ConfirmEmailChangeServiceGrpcKt
import org.springframework.stereotype.Service
import org.axonframework.messaging.commandhandling.gateway.CommandGateway

@Service
class ConfirmEmailChangeGrpcService(
  private val commandGateway: CommandGateway,
  private val userIdResolver: UserIdResolver,
) : ConfirmEmailChangeServiceGrpcKt.ConfirmEmailChangeServiceCoroutineImplBase() {

  override suspend fun confirmEmailChange(request: ConfirmEmailChangeRequest): ConfirmEmailChangeResponse {
    val auth = GrpcAuthContext.current()
    val userId = userIdResolver.resolve(auth)

    commandGateway.confirmEmailChange(
      ConfirmEmailChangeCommand(
        userId = userId,
        token = request.token,
      )
    ).throwIfError()

    return ConfirmEmailChangeResponse.getDefaultInstance()
  }
}
