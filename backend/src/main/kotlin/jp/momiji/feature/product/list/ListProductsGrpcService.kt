package jp.momiji.feature.product.list

import com.google.protobuf.timestamp
import jp.momiji.feature.product.productStatusToProto
import jp.momiji.grpc.momiji.product.list.v1.ListProductsRequest
import jp.momiji.grpc.momiji.product.list.v1.ListProductsResponse
import jp.momiji.grpc.momiji.product.list.v1.ListProductsServiceGrpcKt
import jp.momiji.grpc.momiji.product.list.v1.listProductsResponse
import jp.momiji.grpc.momiji.product.list.v1.product
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.ZoneOffset

@Service
class ListProductsGrpcService(
    private val listProductsQueryService: ListProductsQueryService,
) : ListProductsServiceGrpcKt.ListProductsServiceCoroutineImplBase() {
    override suspend fun listProducts(request: ListProductsRequest): ListProductsResponse {
        val views = listProductsQueryService.findAll()

        return listProductsResponse {
            products.addAll(
                views.map { view ->
                    product {
                        id = view.id
                        brandId = view.brandId
                        name = view.name
                        description = view.description
                        view.imageUrl?.let { imageUrl = it }
                        price = view.price
                        status = productStatusToProto(view.status)
                        createdAt = view.createdAt.toProtoTimestamp()
                        updatedAt = view.updatedAt.toProtoTimestamp()
                    }
                },
            )
        }
    }

    private fun LocalDateTime.toProtoTimestamp() =
        timestamp {
            val instant = this@toProtoTimestamp.toInstant(ZoneOffset.UTC)
            seconds = instant.epochSecond
            nanos = instant.nano
        }
}
