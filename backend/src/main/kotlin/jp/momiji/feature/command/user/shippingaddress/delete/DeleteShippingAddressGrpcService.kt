package jp.momiji.feature.command.user.shippingaddress.delete

import com.github.michaelbull.result.onErr
import com.github.michaelbull.result.onOk
import jp.momiji.config.grpc.GrpcAuthContext
import jp.momiji.domain.Ulid
import jp.momiji.domain.ValidationException
import jp.momiji.feature.command.UserIdResolver
import jp.momiji.feature.command.throwIfError
import jp.momiji.grpc.momiji.user.shippingaddress.delete.DeleteShippingAddressRequest
import jp.momiji.grpc.momiji.user.shippingaddress.delete.DeleteShippingAddressResponse
import jp.momiji.grpc.momiji.user.shippingaddress.delete.DeleteShippingAddressServiceGrpcKt
import org.axonframework.messaging.commandhandling.gateway.CommandGateway
import org.springframework.stereotype.Service

@Service
class DeleteShippingAddressGrpcService(
    private val commandGateway: CommandGateway,
    private val userIdResolver: UserIdResolver,
) : DeleteShippingAddressServiceGrpcKt.DeleteShippingAddressServiceCoroutineImplBase() {
    override suspend fun deleteShippingAddress(request: DeleteShippingAddressRequest): DeleteShippingAddressResponse {
        val accessToken = GrpcAuthContext.current().token
        val userId = userIdResolver.resolve(accessToken)

        Ulid
            .validate(request.shippingAddressId)
            .onErr { error -> throw ValidationException(listOf(error)) }
            .onOk { shippingAddressId ->
                commandGateway
                    .deleteShippingAddress(
                        DeleteShippingAddressCommand(userId = userId, shippingAddressId = shippingAddressId),
                    ).throwIfError()
            }

        return DeleteShippingAddressResponse.getDefaultInstance()
    }
}
