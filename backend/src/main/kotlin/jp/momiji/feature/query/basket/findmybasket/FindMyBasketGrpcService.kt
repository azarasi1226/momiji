package jp.momiji.feature.query.basket.findmybasket

import jp.momiji.config.grpc.GrpcAuthContext
import jp.momiji.feature.command.UserIdResolver
import jp.momiji.feature.query.PagingCondition
import jp.momiji.grpc.momiji.basket.findmybasket.FindMyBasketRequest
import jp.momiji.grpc.momiji.basket.findmybasket.FindMyBasketResponse
import jp.momiji.grpc.momiji.basket.findmybasket.FindMyBasketServiceGrpcKt
import jp.momiji.grpc.momiji.basket.findmybasket.basketItem
import jp.momiji.grpc.momiji.basket.findmybasket.findMyBasketResponse
import jp.momiji.grpc.momiji.common.paging
import org.springframework.stereotype.Service

@Service
class FindMyBasketGrpcService(
    private val userIdResolver: UserIdResolver,
    private val findMyBasketQueryService: FindMyBasketQueryService,
) : FindMyBasketServiceGrpcKt.FindMyBasketServiceCoroutineImplBase() {
    override suspend fun findMyBasket(request: FindMyBasketRequest): FindMyBasketResponse {
        val accessToken = GrpcAuthContext.current().token
        val userId = userIdResolver.resolve(accessToken)
        val query =
            FindMyBasketQuery(
                userId = userId,
                paging = PagingCondition.of(request.pageSize, request.pageNumber),
            )

        val page = findMyBasketQueryService.find(query)

        return findMyBasketResponse {
            paging =
                paging {
                    totalCount = page.paging.totalCount
                    totalPage = page.paging.totalPage
                    pageSize = page.paging.pageSize
                    pageNumber = page.paging.pageNumber
                }
            items.addAll(
                page.items.map { view ->
                    basketItem {
                        productId = view.productId
                        productName = view.productName
                        productPrice = view.productPrice
                        productImageUrl = view.productImageUrl ?: ""
                        itemQuantity = view.itemQuantity
                    }
                },
            )
        }
    }
}
