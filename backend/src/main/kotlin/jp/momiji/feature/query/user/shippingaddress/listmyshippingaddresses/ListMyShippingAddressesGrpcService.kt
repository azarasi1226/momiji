package jp.momiji.feature.query.user.shippingaddress.listmyshippingaddresses

import jp.momiji.config.grpc.GrpcAuthContext
import jp.momiji.feature.command.UserIdResolver
import jp.momiji.grpc.momiji.user.shippingaddress.listmyshippingaddresses.ListMyShippingAddressesRequest
import jp.momiji.grpc.momiji.user.shippingaddress.listmyshippingaddresses.ListMyShippingAddressesResponse
import jp.momiji.grpc.momiji.user.shippingaddress.listmyshippingaddresses.ListMyShippingAddressesServiceGrpcKt
import jp.momiji.grpc.momiji.user.shippingaddress.listmyshippingaddresses.listMyShippingAddressesResponse
import jp.momiji.grpc.momiji.user.shippingaddress.listmyshippingaddresses.shippingAddress
import org.springframework.stereotype.Service

@Service
class ListMyShippingAddressesGrpcService(
    private val userIdResolver: UserIdResolver,
    private val listMyShippingAddressesQueryService: ListMyShippingAddressesQueryService,
) : ListMyShippingAddressesServiceGrpcKt.ListMyShippingAddressesServiceCoroutineImplBase() {
    override suspend fun listMyShippingAddresses(request: ListMyShippingAddressesRequest): ListMyShippingAddressesResponse {
        val accessToken = GrpcAuthContext.current().token
        val userId = userIdResolver.resolve(accessToken)

        val views = listMyShippingAddressesQueryService.findByUserId(userId)

        return listMyShippingAddressesResponse {
            shippingAddresses.addAll(
                views.map { view ->
                    shippingAddress {
                        id = view.id
                        name = view.name
                        phoneNumber = view.phoneNumber
                        postalCode = view.postalCode
                        prefecture = view.prefecture
                        city = view.city
                        streetAddress = view.streetAddress
                        building = view.building
                        deliveryNote = view.deliveryNote
                        isDefault = view.isDefault
                    }
                },
            )
        }
    }
}
