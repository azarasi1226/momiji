package jp.momiji.feature.query.payment.listcards

import jp.momiji.config.grpc.GrpcAuthContext
import jp.momiji.feature.command.UserIdResolver
import jp.momiji.grpc.momiji.payment.listcards.ListCardsRequest
import jp.momiji.grpc.momiji.payment.listcards.ListCardsResponse
import jp.momiji.grpc.momiji.payment.listcards.ListCardsServiceGrpcKt
import jp.momiji.grpc.momiji.payment.listcards.card
import jp.momiji.grpc.momiji.payment.listcards.listCardsResponse
import org.springframework.stereotype.Service

@Service
class ListCardsGrpcService(
    private val userIdResolver: UserIdResolver,
    private val listCardsQueryService: ListCardsQueryService,
) : ListCardsServiceGrpcKt.ListCardsServiceCoroutineImplBase() {
    override suspend fun listCards(request: ListCardsRequest): ListCardsResponse {
        val accessToken = GrpcAuthContext.current().token
        val userId = userIdResolver.resolve(accessToken)

        val cardViews = listCardsQueryService.findByUserId(userId)

        return listCardsResponse {
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
