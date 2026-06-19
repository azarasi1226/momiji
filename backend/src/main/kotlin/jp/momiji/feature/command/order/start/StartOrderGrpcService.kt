package jp.momiji.feature.command.order.start

import de.huxhorn.sulky.ulid.ULID
import jp.momiji.config.grpc.GrpcAuthContext
import jp.momiji.domain.BusinessError
import jp.momiji.domain.BusinessException
import jp.momiji.feature.command.UserIdResolver
import jp.momiji.feature.command.throwIfError
import jp.momiji.grpc.momiji.order.start.StartOrderRequest
import jp.momiji.grpc.momiji.order.start.StartOrderResponse
import jp.momiji.grpc.momiji.order.start.StartOrderServiceGrpcKt
import jp.momiji.grpc.momiji.order.start.startOrderResponse
import org.axonframework.messaging.commandhandling.gateway.CommandGateway
import org.springframework.stereotype.Service

@Service
class StartOrderGrpcService(
    private val commandGateway: CommandGateway,
    private val userIdResolver: UserIdResolver,
    private val orderBasketReader: OrderBasketReader,
) : StartOrderServiceGrpcKt.StartOrderServiceCoroutineImplBase() {
    private val ulid = ULID()

    override suspend fun startOrder(request: StartOrderRequest): StartOrderResponse {
        val accessToken = GrpcAuthContext.current().token
        val userId = userIdResolver.resolve(accessToken)

        // カートの中身を server 側で読み、 注文明細にする（金額・個数はクライアントを信用しない）。 詳細は [OrderBasketReader]。
        val items = orderBasketReader.readItems(userId)
        if (items.isEmpty()) {
            throw BusinessException(BusinessError("カートが空です"))
        }

        // TODO: 冪等キーを利用した仕組みを導入する必要あり
        val orderId = ulid.nextULID()
        commandGateway
            .startOrder(
                StartOrderCommand(
                    id = orderId,
                    userId = userId,
                    shippingAddressId = request.shippingAddressId,
                    expectedTotalAmount = request.expectedTotalAmount,
                    items = items,
                ),
            ).throwIfError()

        return startOrderResponse { this.orderId = orderId }
    }
}
