package jp.momiji.feature.product.create

import com.github.michaelbull.result.onErr
import com.github.michaelbull.result.onOk
import jp.momiji.domain.Ulid
import jp.momiji.domain.ValidationException
import jp.momiji.domain.product.ProductDescription
import jp.momiji.domain.product.ProductImageUrl
import jp.momiji.domain.product.ProductName
import jp.momiji.domain.product.ProductPrice
import jp.momiji.feature.product.imageUrlOrNull
import jp.momiji.feature.throwIfError
import jp.momiji.grpc.momiji.product.create.v1.CreateProductRequest
import jp.momiji.grpc.momiji.product.create.v1.CreateProductResponse
import jp.momiji.grpc.momiji.product.create.v1.CreateProductServiceGrpcKt
import jp.momiji.grpc.momiji.product.create.v1.createProductResponse
import jp.momiji.util.zipOrAccumulate
import org.axonframework.messaging.commandhandling.gateway.CommandGateway
import org.springframework.stereotype.Service

@Service
class CreateProductGrpcService(
    private val commandGateway: CommandGateway,
) : CreateProductServiceGrpcKt.CreateProductServiceCoroutineImplBase() {
    override suspend fun createProduct(request: CreateProductRequest): CreateProductResponse {
        zipOrAccumulate(
            { Ulid.validate(request.id) },
            { Ulid.validate(request.brandId) },
            { ProductName.create(request.name) },
            { ProductDescription.create(request.description) },
            { ProductImageUrl.create(request.imageUrlOrNull) },
            { ProductPrice.create(request.price) },
        ) { id, brandId, name, description, imageUrl, price ->
            CreateProductCommand(
                id = id,
                brandId = brandId,
                name = name,
                description = description,
                imageUrl = imageUrl,
                price = price,
            )
        }.onErr { errors -> throw ValidationException(errors) }
            .onOk { command -> commandGateway.createProduct(command).throwIfError() }

        return createProductResponse { id = request.id }
    }
}
