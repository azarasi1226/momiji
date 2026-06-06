package jp.momiji.feature.product.findbyid

import com.google.protobuf.timestamp
import jp.momiji.domain.BusinessError
import jp.momiji.domain.BusinessException
import jp.momiji.feature.product.productStatusToProto
import jp.momiji.grpc.momiji.product.findbyid.v1.FindProductByIdRequest
import jp.momiji.grpc.momiji.product.findbyid.v1.FindProductByIdResponse
import jp.momiji.grpc.momiji.product.findbyid.v1.FindProductByIdServiceGrpcKt
import jp.momiji.grpc.momiji.product.findbyid.v1.findProductByIdResponse
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.ZoneOffset

@Service
class FindProductByIdGrpcService(
    private val findProductByIdQueryService: FindProductByIdQueryService,
) : FindProductByIdServiceGrpcKt.FindProductByIdServiceCoroutineImplBase() {
    override suspend fun findProductById(request: FindProductByIdRequest): FindProductByIdResponse {
        val product =
            findProductByIdQueryService.findById(request.id)
                ?: throw BusinessException(BusinessError("商品が見つかりません"))

        return findProductByIdResponse {
            id = product.id
            brandId = product.brandId
            name = product.name
            description = product.description
            // image_url は任意。 null のときは proto の optional を未設定のままにする。
            product.imageUrl?.let { imageUrl = it }
            price = product.price
            status = productStatusToProto(product.status)
            createdAt = product.createdAt.toProtoTimestamp()
            updatedAt = product.updatedAt.toProtoTimestamp()
        }
    }

    private fun LocalDateTime.toProtoTimestamp() =
        timestamp {
            val instant = this@toProtoTimestamp.toInstant(ZoneOffset.UTC)
            seconds = instant.epochSecond
            nanos = instant.nano
        }
}
