package jp.momiji.feature.query.basket.findmybasketsummary

import jp.momiji.config.grpc.GrpcAuthContext
import jp.momiji.feature.command.UserIdResolver
import jp.momiji.grpc.momiji.basket.findmybasketsummary.FindMyBasketSummaryRequest
import jp.momiji.grpc.momiji.basket.findmybasketsummary.FindMyBasketSummaryResponse
import jp.momiji.grpc.momiji.basket.findmybasketsummary.FindMyBasketSummaryServiceGrpcKt
import jp.momiji.grpc.momiji.basket.findmybasketsummary.findMyBasketSummaryResponse
import org.springframework.stereotype.Service

@Service
class FindMyBasketSummaryGrpcService(
    private val userIdResolver: UserIdResolver,
    private val findMyBasketSummaryQueryService: FindMyBasketSummaryQueryService,
) : FindMyBasketSummaryServiceGrpcKt.FindMyBasketSummaryServiceCoroutineImplBase() {
    override suspend fun findMyBasketSummary(request: FindMyBasketSummaryRequest): FindMyBasketSummaryResponse {
        val accessToken = GrpcAuthContext.current().token
        val userId = userIdResolver.resolve(accessToken)

        val summary = findMyBasketSummaryQueryService.summarize(userId)

        return findMyBasketSummaryResponse {
            totalQuantity = summary.totalQuantity
            totalTypeCount = summary.totalTypeCount
            totalPrice = summary.totalPrice
        }
    }
}
