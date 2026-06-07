package jp.momiji.feature.command.user.update

import com.github.michaelbull.result.onErr
import com.github.michaelbull.result.onOk
import com.github.michaelbull.result.zipOrAccumulate
import jp.momiji.config.grpc.GrpcAuthContext
import jp.momiji.domain.ValidationException
import jp.momiji.domain.user.Address1
import jp.momiji.domain.user.Address2
import jp.momiji.domain.user.Name
import jp.momiji.domain.user.PhoneNumber
import jp.momiji.domain.user.PostalCode
import jp.momiji.feature.command.throwIfError
import jp.momiji.feature.command.UserIdResolver
import jp.momiji.grpc.momiji.user.update.v1.UpdateUserRequest
import jp.momiji.grpc.momiji.user.update.v1.UpdateUserResponse
import jp.momiji.grpc.momiji.user.update.v1.UpdateUserServiceGrpcKt
import org.axonframework.messaging.commandhandling.gateway.CommandGateway
import org.springframework.stereotype.Service

@Service
class UpdateUserGrpcService(
    private val commandGateway: CommandGateway,
    private val userIdResolver: UserIdResolver,
) : UpdateUserServiceGrpcKt.UpdateUserServiceCoroutineImplBase() {
    override suspend fun updateUser(request: UpdateUserRequest): UpdateUserResponse {
        val accessToken = GrpcAuthContext.current().token
        val userId = userIdResolver.resolve(accessToken)

        val commandResult =
            zipOrAccumulate(
                { Name.create(request.name) },
                { PhoneNumber.create(request.phoneNumber) },
                { PostalCode.create(request.postalCode) },
                { Address1.create(request.address1) },
                { Address2.create(request.address2) },
            ) { name, phoneNumber, postalCode, address1, address2 ->
                UpdateUserCommand(
                    id = userId,
                    name = name,
                    phoneNumber = phoneNumber,
                    postalCode = postalCode,
                    address1 = address1,
                    address2 = address2,
                )
            }

        commandResult
            .onErr { errors -> throw ValidationException(errors) }
            .onOk { command -> commandGateway.updateUser(command).throwIfError() }

        return UpdateUserResponse.getDefaultInstance()
    }
}
