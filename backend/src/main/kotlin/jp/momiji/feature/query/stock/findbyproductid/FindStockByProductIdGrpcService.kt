package jp.momiji.feature.query.stock.findbyproductid

import jp.momiji.grpc.momiji.stock.findbyproductid.v1.FindStockByProductIdRequest
import jp.momiji.grpc.momiji.stock.findbyproductid.v1.FindStockByProductIdResponse
import jp.momiji.grpc.momiji.stock.findbyproductid.v1.FindStockByProductIdServiceGrpcKt
import jp.momiji.grpc.momiji.stock.findbyproductid.v1.findStockByProductIdResponse
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
