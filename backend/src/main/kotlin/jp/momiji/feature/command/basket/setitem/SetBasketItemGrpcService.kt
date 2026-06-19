package jp.momiji.feature.command.basket.setitem

import com.github.michaelbull.result.onErr
import com.github.michaelbull.result.onOk
import com.github.michaelbull.result.zipOrAccumulate
import jp.momiji.config.grpc.GrpcAuthContext
import jp.momiji.domain.Ulid
import jp.momiji.domain.ValidationException
import jp.momiji.domain.basket.BasketItemQuantity
import jp.momiji.feature.command.UserIdResolver
import jp.momiji.feature.command.throwIfError
import jp.momiji.grpc.momiji.basket.setitem.SetBasketItemRequest
import jp.momiji.grpc.momiji.basket.setitem.SetBasketItemResponse
import jp.momiji.grpc.momiji.basket.setitem.SetBasketItemServiceGrpcKt
import org.axonframework.messaging.commandhandling.gateway.CommandGateway
import org.springframework.stereotype.Service

@Service
class SetBasketItemGrpcService(
    private val commandGateway: CommandGateway,
    private val userIdResolver: UserIdResolver,
) : SetBasketItemServiceGrpcKt.SetBasketItemServiceCoroutineImplBase() {
    override suspend fun setBasketItem(request: SetBasketItemRequest): SetBasketItemResponse {
        val accessToken = GrpcAuthContext.current().token
        val userId = userIdResolver.resolve(accessToken)

        zipOrAccumulate(
            { Ulid.validate(request.productId) },
            { BasketItemQuantity.create(request.itemQuantity) },
        ) { productId, quantity ->
            SetBasketItemCommand(userId = userId, productId = productId, itemQuantity = quantity)
        }.onErr { errors -> throw ValidationException(errors) }
            .onOk { command -> commandGateway.setBasketItem(command).throwIfError() }

        return SetBasketItemResponse.getDefaultInstance()
    }
}
