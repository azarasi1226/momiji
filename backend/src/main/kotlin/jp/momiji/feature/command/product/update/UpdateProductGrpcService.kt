package jp.momiji.feature.command.product.update

import com.github.michaelbull.result.onErr
import com.github.michaelbull.result.onOk
import com.github.michaelbull.result.zipOrAccumulate
import jp.momiji.domain.Ulid
import jp.momiji.domain.ValidationException
import jp.momiji.domain.product.ProductDescription
import jp.momiji.domain.product.ProductImageUrl
import jp.momiji.domain.product.ProductName
import jp.momiji.domain.product.ProductPrice
import jp.momiji.feature.command.throwIfError
import jp.momiji.grpc.momiji.product.update.UpdateProductRequest
import jp.momiji.grpc.momiji.product.update.UpdateProductResponse
import jp.momiji.grpc.momiji.product.update.UpdateProductServiceGrpcKt
import org.axonframework.messaging.commandhandling.gateway.CommandGateway
import org.springframework.stereotype.Service

@Service
class UpdateProductGrpcService(
    private val commandGateway: CommandGateway,
) : UpdateProductServiceGrpcKt.UpdateProductServiceCoroutineImplBase() {
    override suspend fun updateProduct(request: UpdateProductRequest): UpdateProductResponse {
        zipOrAccumulate(
            { Ulid.validate(request.id) },
            { ProductName.create(request.name) },
            { ProductDescription.create(request.description) },
            { ProductImageUrl.create(request.imageUrl) },
            { ProductPrice.create(request.price) },
        ) { id, name, description, imageUrl, price ->
            UpdateProductCommand(
                id = id,
                name = name,
                description = description,
                imageUrl = imageUrl,
                price = price,
            )
        }.onErr { errors -> throw ValidationException(errors) }
            .onOk { command -> commandGateway.updateProduct(command).throwIfError() }

        return UpdateProductResponse.getDefaultInstance()
    }
}
