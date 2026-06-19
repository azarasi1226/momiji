package jp.momiji.feature.command.stock.receive

import com.github.michaelbull.result.onErr
import com.github.michaelbull.result.onOk
import com.github.michaelbull.result.zipOrAccumulate
import jp.momiji.domain.Ulid
import jp.momiji.domain.ValidationException
import jp.momiji.domain.stock.ReceiveStockQuantity
import jp.momiji.feature.command.throwIfError
import jp.momiji.grpc.momiji.stock.receive.ReceiveStockRequest
import jp.momiji.grpc.momiji.stock.receive.ReceiveStockResponse
import jp.momiji.grpc.momiji.stock.receive.ReceiveStockServiceGrpcKt
import org.axonframework.messaging.commandhandling.gateway.CommandGateway
import org.springframework.stereotype.Service

@Service
class ReceiveStockGrpcService(
    private val commandGateway: CommandGateway,
) : ReceiveStockServiceGrpcKt.ReceiveStockServiceCoroutineImplBase() {
    override suspend fun receiveStock(request: ReceiveStockRequest): ReceiveStockResponse {
        zipOrAccumulate(
            { Ulid.validate(request.productId) },
            { ReceiveStockQuantity.create(request.quantity) },
        ) { productId, quantity ->
            ReceiveStockCommand(productId = productId, receivedQuantity = quantity)
        }.onErr { errors -> throw ValidationException(errors) }
            .onOk { command -> commandGateway.receiveStock(command).throwIfError() }

        return ReceiveStockResponse.getDefaultInstance()
    }
}
