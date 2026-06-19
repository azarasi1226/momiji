package jp.momiji.feature.command.order.ship

import jp.momiji.feature.command.throwIfError
import jp.momiji.grpc.momiji.order.ship.ShipOrderRequest
import jp.momiji.grpc.momiji.order.ship.ShipOrderResponse
import jp.momiji.grpc.momiji.order.ship.ShipOrderServiceGrpcKt
import jp.momiji.grpc.momiji.order.ship.shipOrderResponse
import org.axonframework.messaging.commandhandling.gateway.CommandGateway
import org.springframework.stereotype.Service

/**
 * 発送手続きの gRPC 入口（admin の発送管理画面が叩く）。 PAID → SHIPPED。
 *
 * 認可は現状「認証のみ」（admin ロール検査は未導入。 brand/product の admin コマンドと同じ）。
 * 不変条件（PAID のときだけ発送・以降は冪等）は [ShipOrderCommandHandler] が DCB sourcing で守る。
 */
@Service
class ShipOrderGrpcService(
    private val commandGateway: CommandGateway,
) : ShipOrderServiceGrpcKt.ShipOrderServiceCoroutineImplBase() {
    override suspend fun shipOrder(request: ShipOrderRequest): ShipOrderResponse {
        commandGateway.shipOrder(ShipOrderCommand(orderId = request.orderId)).throwIfError()
        return shipOrderResponse {}
    }
}
