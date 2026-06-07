package jp.momiji.feature.command.basket.clear

import jp.momiji.config.grpc.GrpcAuthContext
import jp.momiji.feature.command.UserIdResolver
import jp.momiji.feature.command.throwIfError
import jp.momiji.grpc.momiji.basket.clear.v1.ClearBasketRequest
import jp.momiji.grpc.momiji.basket.clear.v1.ClearBasketResponse
import jp.momiji.grpc.momiji.basket.clear.v1.ClearBasketServiceGrpcKt
import org.axonframework.messaging.commandhandling.gateway.CommandGateway
import org.springframework.stereotype.Service

@Service
class ClearBasketGrpcService(
    private val commandGateway: CommandGateway,
    private val userIdResolver: UserIdResolver,
) : ClearBasketServiceGrpcKt.ClearBasketServiceCoroutineImplBase() {
    override suspend fun clearBasket(request: ClearBasketRequest): ClearBasketResponse {
        val userId = userIdResolver.resolve(GrpcAuthContext.current().token)

        commandGateway
            .clearBasket(ClearBasketCommand(userId = userId))
            .throwIfError()

        return ClearBasketResponse.getDefaultInstance()
    }
}
