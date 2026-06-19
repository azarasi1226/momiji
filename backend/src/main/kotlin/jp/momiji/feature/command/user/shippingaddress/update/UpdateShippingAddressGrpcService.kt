package jp.momiji.feature.command.user.shippingaddress.update

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
import jp.momiji.grpc.momiji.user.shippingaddress.update.UpdateShippingAddressRequest
import jp.momiji.grpc.momiji.user.shippingaddress.update.UpdateShippingAddressResponse
import jp.momiji.grpc.momiji.user.shippingaddress.update.UpdateShippingAddressServiceGrpcKt
import jp.momiji.util.zipOrAccumulate
import org.axonframework.messaging.commandhandling.gateway.CommandGateway
import org.springframework.stereotype.Service

@Service
class UpdateShippingAddressGrpcService(
    private val commandGateway: CommandGateway,
    private val userIdResolver: UserIdResolver,
) : UpdateShippingAddressServiceGrpcKt.UpdateShippingAddressServiceCoroutineImplBase() {
    override suspend fun updateShippingAddress(request: UpdateShippingAddressRequest): UpdateShippingAddressResponse {
        val accessToken = GrpcAuthContext.current().token
        val userId = userIdResolver.resolve(accessToken)

        zipOrAccumulate(
            { Ulid.validate(request.shippingAddressId) },
            { Name.create(request.name) },
            { PhoneNumber.create(request.phoneNumber) },
            { PostalCode.create(request.postalCode) },
            { Prefecture.create(request.prefecture) },
            { City.create(request.city) },
            { StreetAddress.create(request.streetAddress) },
            { Building.create(request.building) },
            { DeliveryNote.create(request.deliveryNote) },
        ) { shippingAddressId, name, phoneNumber, postalCode, prefecture, city, streetAddress, building, deliveryNote ->
            UpdateShippingAddressCommand(
                userId = userId,
                shippingAddressId = shippingAddressId,
                name = name,
                phoneNumber = phoneNumber,
                postalCode = postalCode,
                prefecture = prefecture,
                city = city,
                streetAddress = streetAddress,
                building = building,
                deliveryNote = deliveryNote,
            )
        }.onErr { errors -> throw ValidationException(errors) }
            .onOk { command -> commandGateway.updateShippingAddress(command).throwIfError() }

        return UpdateShippingAddressResponse.getDefaultInstance()
    }
}
