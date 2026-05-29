package jp.momiji.feature.user.update

import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import com.github.michaelbull.result.zipOrAccumulate
import jp.momiji.config.grpc.GrpcAuthContext
import jp.momiji.domain.ValidationException
import jp.momiji.domain.user.Address1
import jp.momiji.domain.user.Address2
import jp.momiji.domain.user.Name
import jp.momiji.domain.user.PhoneNumber
import jp.momiji.domain.user.PostalCode
import jp.momiji.feature.throwIfError
import jp.momiji.feature.user.UserIdResolver
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
        val auth = GrpcAuthContext.current()
        val userId = userIdResolver.resolve(auth)

        // 各値オブジェクトを検証し、 全エラーを蓄積して 1 つの Command に組み立てる。
        // 失敗時は ValidationException を投げて grpcExceptionHandler 経由で INVALID_ARGUMENT に変換。
        // zipOrAccumulate は kotlin-result 標準。 lazy lambda で受け取る形式。
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
            .onFailure { errors -> throw ValidationException(errors) }
            .onSuccess { command -> commandGateway.updateUser(command).throwIfError() }

        return UpdateUserResponse.getDefaultInstance()
    }
}
