package jp.momiji.feature.brand.delete

import com.github.michaelbull.result.onErr
import com.github.michaelbull.result.onOk
import jp.momiji.domain.Ulid
import jp.momiji.domain.ValidationException
import jp.momiji.feature.throwIfError
import jp.momiji.grpc.momiji.brand.delete.v1.DeleteBrandRequest
import jp.momiji.grpc.momiji.brand.delete.v1.DeleteBrandResponse
import jp.momiji.grpc.momiji.brand.delete.v1.DeleteBrandServiceGrpcKt
import org.axonframework.messaging.commandhandling.gateway.CommandGateway
import org.springframework.stereotype.Service

@Service
class DeleteBrandGrpcService(
    private val commandGateway: CommandGateway,
) : DeleteBrandServiceGrpcKt.DeleteBrandServiceCoroutineImplBase() {
    override suspend fun deleteBrand(request: DeleteBrandRequest): DeleteBrandResponse {
        Ulid
            .validate(request.id)
            .onErr { error -> throw ValidationException(listOf(error)) }
            .onOk { id ->
                commandGateway
                    .deleteBrand(DeleteBrandCommand(id = id))
                    .throwIfError()
            }

        return DeleteBrandResponse.getDefaultInstance()
    }
}
