package jp.momiji.feature.query.payment.listmycards

import jp.momiji.config.grpc.GrpcAuthContext
import jp.momiji.feature.command.UserIdResolver
import jp.momiji.grpc.momiji.payment.listmycards.ListMyCardsRequest
import jp.momiji.grpc.momiji.payment.listmycards.ListMyCardsResponse
import jp.momiji.grpc.momiji.payment.listmycards.ListMyCardsServiceGrpcKt
import jp.momiji.grpc.momiji.payment.listmycards.card
import jp.momiji.grpc.momiji.payment.listmycards.listMyCardsResponse
import org.springframework.stereotype.Service

@Service
class ListMyCardsGrpcService(
    private val userIdResolver: UserIdResolver,
    private val listMyCardsQueryService: ListMyCardsQueryService,
) : ListMyCardsServiceGrpcKt.ListMyCardsServiceCoroutineImplBase() {
    override suspend fun listMyCards(request: ListMyCardsRequest): ListMyCardsResponse {
        val accessToken = GrpcAuthContext.current().token
        val userId = userIdResolver.resolve(accessToken)

        val cardViews = listMyCardsQueryService.findByUserId(userId)

        return listMyCardsResponse {
            cards.addAll(
                cardViews.map { view ->
                    card {
                        id = view.id
                        brand = view.brand
                        last4 = view.last4
                        expMonth = view.expMonth
                        expYear = view.expYear
                        isDefault = view.isDefault
                    }
                },
            )
        }
    }
}
