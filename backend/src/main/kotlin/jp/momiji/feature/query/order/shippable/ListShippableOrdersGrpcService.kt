package jp.momiji.feature.query.order.shippable

import jp.momiji.grpc.momiji.order.shippable.ListShippableOrdersRequest
import jp.momiji.grpc.momiji.order.shippable.ListShippableOrdersResponse
import jp.momiji.grpc.momiji.order.shippable.ListShippableOrdersServiceGrpcKt
import jp.momiji.grpc.momiji.order.shippable.item
import jp.momiji.grpc.momiji.order.shippable.listShippableOrdersResponse
import jp.momiji.grpc.momiji.order.shippable.shippableOrder
import jp.momiji.grpc.momiji.order.shippable.shippingAddress
import jp.momiji.util.toProtoTimestamp
import org.springframework.stereotype.Service

@Service
class ListShippableOrdersGrpcService(
    private val listShippableOrdersQueryService: ListShippableOrdersQueryService,
) : ListShippableOrdersServiceGrpcKt.ListShippableOrdersServiceCoroutineImplBase() {
    override suspend fun listShippableOrders(request: ListShippableOrdersRequest): ListShippableOrdersResponse {
        val views = listShippableOrdersQueryService.findShippable()

        return listShippableOrdersResponse {
            orders.addAll(
                views.map { view ->
                    shippableOrder {
                        orderId = view.orderId
                        shippingAddress =
                            shippingAddress {
                                recipientName = view.shippingAddress.recipientName
                                phoneNumber = view.shippingAddress.phoneNumber
                                postalCode = view.shippingAddress.postalCode
                                prefecture = view.shippingAddress.prefecture
                                city = view.shippingAddress.city
                                streetAddress = view.shippingAddress.streetAddress
                                building = view.shippingAddress.building
                                deliveryNote = view.shippingAddress.deliveryNote
                            }
                        totalAmount = view.totalAmount
                        createdAt = view.createdAt.toProtoTimestamp()
                        items.addAll(
                            view.items.map { viewItem ->
                                item {
                                    name = viewItem.name
                                    quantity = viewItem.quantity
                                }
                            },
                        )
                    }
                },
            )
        }
    }
}
