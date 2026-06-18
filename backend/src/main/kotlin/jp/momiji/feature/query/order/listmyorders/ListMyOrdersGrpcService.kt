package jp.momiji.feature.query.order.listmyorders

import jp.momiji.config.grpc.GrpcAuthContext
import jp.momiji.feature.command.UserIdResolver
import jp.momiji.feature.query.PagingCondition
import jp.momiji.feature.query.order.orderStatusToProto
import jp.momiji.grpc.momiji.common.v1.paging
import jp.momiji.grpc.momiji.order.listmyorders.v1.ListMyOrdersRequest
import jp.momiji.grpc.momiji.order.listmyorders.v1.ListMyOrdersResponse
import jp.momiji.grpc.momiji.order.listmyorders.v1.ListMyOrdersServiceGrpcKt
import jp.momiji.grpc.momiji.order.listmyorders.v1.item
import jp.momiji.grpc.momiji.order.listmyorders.v1.listMyOrdersResponse
import jp.momiji.grpc.momiji.order.listmyorders.v1.order
import jp.momiji.util.toProtoTimestamp
import jp.momiji.util.toUtcLocalDateTime
import org.springframework.stereotype.Service

@Service
class ListMyOrdersGrpcService(
    private val userIdResolver: UserIdResolver,
    private val listMyOrdersQueryService: ListMyOrdersQueryService,
) : ListMyOrdersServiceGrpcKt.ListMyOrdersServiceCoroutineImplBase() {
    override suspend fun listMyOrders(request: ListMyOrdersRequest): ListMyOrdersResponse {
        val accessToken = GrpcAuthContext.current().token
        val userId = userIdResolver.resolve(accessToken)

        val page =
            listMyOrdersQueryService.findByUserId(
                userId = userId,
                paging = PagingCondition.of(request.pageSize, request.pageNumber),
                // optional フィールドは presence を見てから読む（未指定なら範囲をその側で開放）。
                createdFrom = if (request.hasCreatedFrom()) request.createdFrom.toUtcLocalDateTime() else null,
                createdTo = if (request.hasCreatedTo()) request.createdTo.toUtcLocalDateTime() else null,
            )

        return listMyOrdersResponse {
            paging =
                paging {
                    totalCount = page.paging.totalCount
                    totalPage = page.paging.totalPage
                    pageSize = page.paging.pageSize
                    pageNumber = page.paging.pageNumber
                }
            orders.addAll(
                page.items.map { view ->
                    order {
                        orderId = view.orderId
                        status = orderStatusToProto(view.status)
                        totalAmount = view.totalAmount
                        createdAt = view.createdAt.toProtoTimestamp()
                        items.addAll(
                            view.items.map { viewItem ->
                                item {
                                    productId = viewItem.productId
                                    name = viewItem.name
                                    unitPrice = viewItem.unitPrice
                                    quantity = viewItem.quantity
                                    imageUrl = viewItem.imageUrl ?: ""
                                }
                            },
                        )
                    }
                },
            )
        }
    }
}
