package jp.momiji.feature.command.user.shippingaddress.changedefault

import com.github.michaelbull.result.onErr
import com.github.michaelbull.result.onOk
import jp.momiji.config.grpc.GrpcAuthContext
import jp.momiji.domain.Ulid
import jp.momiji.domain.ValidationException
import jp.momiji.feature.command.UserIdResolver
import jp.momiji.feature.command.throwIfError
import jp.momiji.grpc.momiji.user.shippingaddress.changedefault.v1.ChangeDefaultShippingAddressRequest
import jp.momiji.grpc.momiji.user.shippingaddress.changedefault.v1.ChangeDefaultShippingAddressResponse
import jp.momiji.grpc.momiji.user.shippingaddress.changedefault.v1.ChangeDefaultShippingAddressServiceGrpcKt
import org.axonframework.messaging.commandhandling.gateway.CommandGateway
import org.springframework.stereotype.Service

@Service
class ChangeDefaultShippingAddressGrpcService(
    private val commandGateway: CommandGateway,
    private val userIdResolver: UserIdResolver,
) : ChangeDefaultShippingAddressServiceGrpcKt.ChangeDefaultShippingAddressServiceCoroutineImplBase() {
    override suspend fun changeDefaultShippingAddress(request: ChangeDefaultShippingAddressRequest): ChangeDefaultShippingAddressResponse {
        val accessToken = GrpcAuthContext.current().token
        val userId = userIdResolver.resolve(accessToken)

        Ulid
            .validate(request.shippingAddressId)
            .onErr { error -> throw ValidationException(listOf(error)) }
            .onOk { shippingAddressId ->
                commandGateway
                    .changeDefaultShippingAddress(
                        ChangeDefaultShippingAddressCommand(userId = userId, shippingAddressId = shippingAddressId),
                    ).throwIfError()
            }

        return ChangeDefaultShippingAddressResponse.getDefaultInstance()
    }
}
