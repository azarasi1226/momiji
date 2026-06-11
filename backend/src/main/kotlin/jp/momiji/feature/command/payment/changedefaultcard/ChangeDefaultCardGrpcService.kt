package jp.momiji.feature.command.payment.changedefaultcard

import jp.momiji.config.grpc.GrpcAuthContext
import jp.momiji.feature.command.UserIdResolver
import jp.momiji.feature.command.throwIfError
import jp.momiji.grpc.momiji.payment.changedefaultcard.v1.ChangeDefaultCardRequest
import jp.momiji.grpc.momiji.payment.changedefaultcard.v1.ChangeDefaultCardResponse
import jp.momiji.grpc.momiji.payment.changedefaultcard.v1.ChangeDefaultCardServiceGrpcKt
import org.axonframework.messaging.commandhandling.gateway.CommandGateway
import org.springframework.stereotype.Service

@Service
class ChangeDefaultCardGrpcService(
    private val commandGateway: CommandGateway,
    private val userIdResolver: UserIdResolver,
) : ChangeDefaultCardServiceGrpcKt.ChangeDefaultCardServiceCoroutineImplBase() {
    override suspend fun changeDefaultCard(request: ChangeDefaultCardRequest): ChangeDefaultCardResponse {
        val accessToken = GrpcAuthContext.current().token
        val userId = userIdResolver.resolve(accessToken)

        commandGateway
            .changeDefaultCard(
                ChangeDefaultCardCommand(userId = userId, paymentMethodId = request.paymentMethodId),
            ).throwIfError()

        return ChangeDefaultCardResponse.getDefaultInstance()
    }
}
