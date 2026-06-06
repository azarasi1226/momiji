package jp.momiji.feature.product

import jp.momiji.grpc.momiji.product.create.v1.CreateProductRequest
import jp.momiji.grpc.momiji.product.update.v1.UpdateProductRequest

internal val CreateProductRequest.imageUrlOrNull: String?
    get() = if (hasImageUrl()) imageUrl else null

internal val UpdateProductRequest.imageUrlOrNull: String?
    get() = if (hasImageUrl()) imageUrl else null
