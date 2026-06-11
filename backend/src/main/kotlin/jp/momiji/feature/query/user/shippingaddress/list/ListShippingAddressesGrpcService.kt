package jp.momiji.feature.query.user.shippingaddress.list

import jp.momiji.config.grpc.GrpcAuthContext
import jp.momiji.feature.command.UserIdResolver
import jp.momiji.grpc.momiji.user.shippingaddress.list.v1.ListShippingAddressesRequest
import jp.momiji.grpc.momiji.user.shippingaddress.list.v1.ListShippingAddressesResponse
import jp.momiji.grpc.momiji.user.shippingaddress.list.v1.ListShippingAddressesServiceGrpcKt
import jp.momiji.grpc.momiji.user.shippingaddress.list.v1.listShippingAddressesResponse
import jp.momiji.grpc.momiji.user.shippingaddress.list.v1.shippingAddress
import org.springframework.stereotype.Service

@Service
class ListShippingAddressesGrpcService(
    private val userIdResolver: UserIdResolver,
    private val listShippingAddressesQueryService: ListShippingAddressesQueryService,
) : ListShippingAddressesServiceGrpcKt.ListShippingAddressesServiceCoroutineImplBase() {
    override suspend fun listShippingAddresses(request: ListShippingAddressesRequest): ListShippingAddressesResponse {
        val accessToken = GrpcAuthContext.current().token
        val userId = userIdResolver.resolve(accessToken)

        val views = listShippingAddressesQueryService.findByUserId(userId)

        return listShippingAddressesResponse {
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
