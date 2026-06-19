package jp.momiji.feature.command.basket.deleteitem

import com.github.michaelbull.result.onErr
import com.github.michaelbull.result.onOk
import jp.momiji.config.grpc.GrpcAuthContext
import jp.momiji.domain.Ulid
import jp.momiji.domain.ValidationException
import jp.momiji.feature.command.UserIdResolver
import jp.momiji.feature.command.throwIfError
import jp.momiji.grpc.momiji.basket.deleteitem.DeleteBasketItemRequest
import jp.momiji.grpc.momiji.basket.deleteitem.DeleteBasketItemResponse
import jp.momiji.grpc.momiji.basket.deleteitem.DeleteBasketItemServiceGrpcKt
import org.axonframework.messaging.commandhandling.gateway.CommandGateway
import org.springframework.stereotype.Service

@Service
class DeleteBasketItemGrpcService(
    private val commandGateway: CommandGateway,
    private val userIdResolver: UserIdResolver,
) : DeleteBasketItemServiceGrpcKt.DeleteBasketItemServiceCoroutineImplBase() {
    override suspend fun deleteBasketItem(request: DeleteBasketItemRequest): DeleteBasketItemResponse {
        val accessToken = GrpcAuthContext.current().token
        val userId = userIdResolver.resolve(accessToken)

        Ulid
            .validate(request.productId)
            .onErr { error -> throw ValidationException(listOf(error)) }
            .onOk { productId ->
                commandGateway
                    .deleteBasketItem(DeleteBasketItemCommand(userId = userId, productId = productId))
                    .throwIfError()
            }

        return DeleteBasketItemResponse.getDefaultInstance()
    }
}
