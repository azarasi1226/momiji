package jp.momiji.feature.command.brand.create

import com.github.michaelbull.result.onErr
import com.github.michaelbull.result.onOk
import com.github.michaelbull.result.zipOrAccumulate
import jp.momiji.domain.Ulid
import jp.momiji.domain.ValidationException
import jp.momiji.domain.brand.BrandDescription
import jp.momiji.domain.brand.BrandName
import jp.momiji.feature.command.throwIfError
import jp.momiji.grpc.momiji.brand.create.CreateBrandRequest
import jp.momiji.grpc.momiji.brand.create.CreateBrandResponse
import jp.momiji.grpc.momiji.brand.create.CreateBrandServiceGrpcKt
import jp.momiji.grpc.momiji.brand.create.createBrandResponse
import org.axonframework.messaging.commandhandling.gateway.CommandGateway
import org.springframework.stereotype.Service

@Service
class CreateBrandGrpcService(
    private val commandGateway: CommandGateway,
) : CreateBrandServiceGrpcKt.CreateBrandServiceCoroutineImplBase() {
    override suspend fun createBrand(request: CreateBrandRequest): CreateBrandResponse {
        zipOrAccumulate(
            { Ulid.validate(request.id) },
            { BrandName.create(request.name) },
            { BrandDescription.create(request.description) },
        ) { id, name, description ->
            CreateBrandCommand(id = id, name = name, description = description)
        }.onErr { errors -> throw ValidationException(errors) }
            .onOk { command -> commandGateway.createBrand(command).throwIfError() }

        return createBrandResponse { id = request.id }
    }
}
