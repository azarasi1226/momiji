package jp.momiji.feature.command.payment.deletecard

import jp.momiji.config.grpc.GrpcAuthContext
import jp.momiji.feature.command.UserIdResolver
import jp.momiji.feature.command.throwIfError
import jp.momiji.grpc.momiji.payment.deletecard.DeleteCardRequest
import jp.momiji.grpc.momiji.payment.deletecard.DeleteCardResponse
import jp.momiji.grpc.momiji.payment.deletecard.DeleteCardServiceGrpcKt
import org.axonframework.messaging.commandhandling.gateway.CommandGateway
import org.springframework.stereotype.Service

@Service
class DeleteCardGrpcService(
    private val commandGateway: CommandGateway,
    private val userIdResolver: UserIdResolver,
) : DeleteCardServiceGrpcKt.DeleteCardServiceCoroutineImplBase() {
    override suspend fun deleteCard(request: DeleteCardRequest): DeleteCardResponse {
        val accessToken = GrpcAuthContext.current().token
        val userId = userIdResolver.resolve(accessToken)

        commandGateway
            .deleteCard(
                DeleteCardCommand(userId = userId, paymentMethodId = request.paymentMethodId),
            ).throwIfError()

        return DeleteCardResponse.getDefaultInstance()
    }
}
