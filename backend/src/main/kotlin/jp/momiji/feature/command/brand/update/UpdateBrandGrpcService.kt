package jp.momiji.feature.command.brand.update

import com.github.michaelbull.result.onErr
import com.github.michaelbull.result.onOk
import com.github.michaelbull.result.zipOrAccumulate
import jp.momiji.domain.Ulid
import jp.momiji.domain.ValidationException
import jp.momiji.domain.brand.BrandDescription
import jp.momiji.domain.brand.BrandName
import jp.momiji.feature.command.throwIfError
import jp.momiji.grpc.momiji.brand.update.UpdateBrandRequest
import jp.momiji.grpc.momiji.brand.update.UpdateBrandResponse
import jp.momiji.grpc.momiji.brand.update.UpdateBrandServiceGrpcKt
import org.axonframework.messaging.commandhandling.gateway.CommandGateway
import org.springframework.stereotype.Service

@Service
class UpdateBrandGrpcService(
    private val commandGateway: CommandGateway,
) : UpdateBrandServiceGrpcKt.UpdateBrandServiceCoroutineImplBase() {
    override suspend fun updateBrand(request: UpdateBrandRequest): UpdateBrandResponse {
        zipOrAccumulate(
            { Ulid.validate(request.id) },
            { BrandName.create(request.name) },
            { BrandDescription.create(request.description) },
        ) { id, name, description ->
            UpdateBrandCommand(id = id, name = name, description = description)
        }.onErr { errors -> throw ValidationException(errors) }
            .onOk { command -> commandGateway.updateBrand(command).throwIfError() }

        return UpdateBrandResponse.getDefaultInstance()
    }
}
