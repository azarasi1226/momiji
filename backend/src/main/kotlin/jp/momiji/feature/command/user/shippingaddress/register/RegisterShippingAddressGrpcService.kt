package jp.momiji.feature.command.user.shippingaddress.register

import com.github.michaelbull.result.onErr
import com.github.michaelbull.result.onOk
import jp.momiji.config.grpc.GrpcAuthContext
import jp.momiji.domain.Ulid
import jp.momiji.domain.ValidationException
import jp.momiji.domain.user.Building
import jp.momiji.domain.user.City
import jp.momiji.domain.user.DeliveryNote
import jp.momiji.domain.user.Name
import jp.momiji.domain.user.PhoneNumber
import jp.momiji.domain.user.PostalCode
import jp.momiji.domain.user.Prefecture
import jp.momiji.domain.user.StreetAddress
import jp.momiji.feature.command.UserIdResolver
import jp.momiji.feature.command.throwIfError
import jp.momiji.grpc.momiji.user.shippingaddress.register.v1.RegisterShippingAddressRequest
import jp.momiji.grpc.momiji.user.shippingaddress.register.v1.RegisterShippingAddressResponse
import jp.momiji.grpc.momiji.user.shippingaddress.register.v1.RegisterShippingAddressServiceGrpcKt
import jp.momiji.grpc.momiji.user.shippingaddress.register.v1.registerShippingAddressResponse
import jp.momiji.util.zipOrAccumulate
import org.axonframework.messaging.commandhandling.gateway.CommandGateway
import org.springframework.stereotype.Service

@Service
class RegisterShippingAddressGrpcService(
    private val commandGateway: CommandGateway,
    private val userIdResolver: UserIdResolver,
) : RegisterShippingAddressServiceGrpcKt.RegisterShippingAddressServiceCoroutineImplBase() {
    override suspend fun registerShippingAddress(request: RegisterShippingAddressRequest): RegisterShippingAddressResponse {
        val accessToken = GrpcAuthContext.current().token
        val userId = userIdResolver.resolve(accessToken)

        zipOrAccumulate(
            { Ulid.validate(request.id) },
            { Name.create(request.name) },
            { PhoneNumber.create(request.phoneNumber) },
            { PostalCode.create(request.postalCode) },
            { Prefecture.create(request.prefecture) },
            { City.create(request.city) },
            { StreetAddress.create(request.streetAddress) },
            { Building.create(request.building) },
            { DeliveryNote.create(request.deliveryNote) },
        ) { id, name, phoneNumber, postalCode, prefecture, city, streetAddress, building, deliveryNote ->
            RegisterShippingAddressCommand(
                userId = userId,
                id = id,
                name = name,
                phoneNumber = phoneNumber,
                postalCode = postalCode,
                prefecture = prefecture,
                city = city,
                streetAddress = streetAddress,
                building = building,
                deliveryNote = deliveryNote,
                makeDefault = request.makeDefault,
            )
        }.onErr { errors -> throw ValidationException(errors) }
            .onOk { command -> commandGateway.registerShippingAddress(command).throwIfError() }

        return registerShippingAddressResponse { id = request.id }
    }
}
