package jp.momiji.feature.command.stock.adjust

import com.github.michaelbull.result.andThen
import com.github.michaelbull.result.map
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.onErr
import com.github.michaelbull.result.onOk
import com.github.michaelbull.result.zipOrAccumulate
import jp.momiji.domain.Ulid
import jp.momiji.domain.ValidationException
import jp.momiji.domain.stock.AdjustStockQuantity
import jp.momiji.domain.stock.StockAdjustment
import jp.momiji.feature.command.stock.stockAdjustmentReasonFromProto
import jp.momiji.feature.command.throwIfError
import jp.momiji.grpc.momiji.stock.adjust.v1.AdjustStockRequest
import jp.momiji.grpc.momiji.stock.adjust.v1.AdjustStockResponse
import jp.momiji.grpc.momiji.stock.adjust.v1.AdjustStockServiceGrpcKt
import org.axonframework.messaging.commandhandling.gateway.CommandGateway
import org.springframework.stereotype.Service

@Service
class AdjustStockGrpcService(
    private val commandGateway: CommandGateway,
) : AdjustStockServiceGrpcKt.AdjustStockServiceCoroutineImplBase() {
    override suspend fun adjustStock(request: AdjustStockRequest): AdjustStockResponse {
        // ① 各フィールドを独立に検証（productId / 数量 / 理由）。
        zipOrAccumulate(
            { Ulid.validate(request.productId) },
            { AdjustStockQuantity.create(request.quantity) },
            { stockAdjustmentReasonFromProto(request.reason) },
        ) { productId, quantity, reason ->
            Triple(productId, quantity, reason)
        }.andThen { (productId, quantity, reason) ->
            // ② 数量と理由の組み合わせ検証（増加できるのは棚卸しだけ）。 ①が全部 OK のときだけ走る。
            StockAdjustment
                .create(quantity, reason)
                .map { adjustment -> AdjustStockCommand(productId = productId, adjustment = adjustment) }
                .mapError { error -> listOf(error) }
        }.onErr { errors -> throw ValidationException(errors) }
            .onOk { command -> commandGateway.adjustStock(command).throwIfError() }

        return AdjustStockResponse.getDefaultInstance()
    }
}
