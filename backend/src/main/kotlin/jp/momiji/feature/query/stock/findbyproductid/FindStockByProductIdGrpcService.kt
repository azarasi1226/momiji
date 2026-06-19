package jp.momiji.feature.query.stock.findbyproductid

import jp.momiji.grpc.momiji.stock.findbyproductid.FindStockByProductIdRequest
import jp.momiji.grpc.momiji.stock.findbyproductid.FindStockByProductIdResponse
import jp.momiji.grpc.momiji.stock.findbyproductid.FindStockByProductIdServiceGrpcKt
import jp.momiji.grpc.momiji.stock.findbyproductid.findStockByProductIdResponse
import org.springframework.stereotype.Service

@Service
class FindStockByProductIdGrpcService(
    private val findStockByProductIdQueryService: FindStockByProductIdQueryService,
) : FindStockByProductIdServiceGrpcKt.FindStockByProductIdServiceCoroutineImplBase() {
    override suspend fun findStockByProductId(request: FindStockByProductIdRequest): FindStockByProductIdResponse {
        val view = findStockByProductIdQueryService.findByProductId(request.productId)

        return findStockByProductIdResponse {
            onHand = view.onHand
            reserved = view.reserved
            available = view.available
        }
    }
}
