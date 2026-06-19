package jp.momiji.feature.query.order.findmyorder

import jp.momiji.config.grpc.GrpcAuthContext
import jp.momiji.domain.BusinessError
import jp.momiji.domain.BusinessException
import jp.momiji.feature.command.UserIdResolver
import jp.momiji.feature.query.order.orderStatusToProto
import jp.momiji.grpc.momiji.order.findmyorder.FindMyOrderRequest
import jp.momiji.grpc.momiji.order.findmyorder.FindMyOrderResponse
import jp.momiji.grpc.momiji.order.findmyorder.FindMyOrderServiceGrpcKt
import jp.momiji.grpc.momiji.order.findmyorder.findMyOrderResponse
import jp.momiji.grpc.momiji.order.findmyorder.item
import jp.momiji.grpc.momiji.order.findmyorder.paymentMethod
import jp.momiji.grpc.momiji.order.findmyorder.shippingAddress
import jp.momiji.util.toProtoTimestamp
import org.springframework.stereotype.Service

@Service
class FindMyOrderGrpcService(
    private val userIdResolver: UserIdResolver,
    private val findMyOrderQueryService: FindMyOrderQueryService,
) : FindMyOrderServiceGrpcKt.FindMyOrderServiceCoroutineImplBase() {
    override suspend fun findMyOrder(request: FindMyOrderRequest): FindMyOrderResponse {
        val accessToken = GrpcAuthContext.current().token
        val userId = userIdResolver.resolve(accessToken)

        // 本人の注文でなければ null → 「見つかりません」（他人の注文の存在を漏らさない）。
        val view =
            findMyOrderQueryService.findByIdForUser(orderId = request.orderId, userId = userId)
                ?: throw BusinessException(BusinessError("注文が見つかりません"))

        return findMyOrderResponse {
            orderId = view.orderId
            status = orderStatusToProto(view.status)
            totalAmount = view.totalAmount
            createdAt = view.createdAt.toProtoTimestamp()
            updatedAt = view.updatedAt.toProtoTimestamp()
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
            // 使用カードがある場合だけ optional フィールドを詰める（無ければ未設定のまま）。
            view.paymentMethod?.let { pm ->
                paymentMethod =
                    paymentMethod {
                        id = pm.id
                        brand = pm.brand
                        last4 = pm.last4
                    }
            }
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
    }
}
