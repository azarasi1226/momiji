package jp.momiji.feature.brand.list

import com.google.protobuf.timestamp
import jp.momiji.feature.brand.brandStatusToProto
import jp.momiji.grpc.momiji.brand.list.v1.ListBrandsRequest
import jp.momiji.grpc.momiji.brand.list.v1.ListBrandsResponse
import jp.momiji.grpc.momiji.brand.list.v1.ListBrandsServiceGrpcKt
import jp.momiji.grpc.momiji.brand.list.v1.brand
import jp.momiji.grpc.momiji.brand.list.v1.listBrandsResponse
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.ZoneOffset

@Service
class ListBrandsGrpcService(
    private val listBrandsQueryService: ListBrandsQueryService,
) : ListBrandsServiceGrpcKt.ListBrandsServiceCoroutineImplBase() {
    override suspend fun listBrands(request: ListBrandsRequest): ListBrandsResponse {
        val views = listBrandsQueryService.findAll()

        return listBrandsResponse {
            brands.addAll(
                views.map { view ->
                    brand {
                        id = view.id
                        name = view.name
                        description = view.description
                        status = brandStatusToProto(view.status)
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
