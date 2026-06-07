package jp.momiji.feature.query.basket.findbyid

import jp.momiji.config.grpc.GrpcAuthContext
import jp.momiji.feature.command.UserIdResolver
import jp.momiji.feature.query.PagingCondition
import jp.momiji.grpc.momiji.basket.findbyid.v1.FindBasketByIdRequest
import jp.momiji.grpc.momiji.basket.findbyid.v1.FindBasketByIdResponse
import jp.momiji.grpc.momiji.basket.findbyid.v1.FindBasketByIdServiceGrpcKt
import jp.momiji.grpc.momiji.basket.findbyid.v1.basketItem
import jp.momiji.grpc.momiji.basket.findbyid.v1.findBasketByIdResponse
import jp.momiji.grpc.momiji.common.v1.paging
import org.springframework.stereotype.Service

@Service
class FindBasketByIdGrpcService(
    private val userIdResolver: UserIdResolver,
    private val findBasketByIdQueryService: FindBasketByIdQueryService,
) : FindBasketByIdServiceGrpcKt.FindBasketByIdServiceCoroutineImplBase() {
    override suspend fun findBasketById(request: FindBasketByIdRequest): FindBasketByIdResponse {
        val accessToken = GrpcAuthContext.current().token
        val userId = userIdResolver.resolve(accessToken)
        val query =
            FindBasketByIdQuery(
                userId = userId,
                paging = PagingCondition.of(request.pageSize, request.pageNumber),
            )

        val page = findBasketByIdQueryService.findById(query)

        return findBasketByIdResponse {
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
