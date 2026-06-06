package jp.momiji.feature.brand.list

import jp.momiji.feature.brand.brandStatusToProto
import jp.momiji.grpc.momiji.brand.list.v1.ListBrandsRequest
import jp.momiji.grpc.momiji.brand.list.v1.ListBrandsResponse
import jp.momiji.grpc.momiji.brand.list.v1.ListBrandsServiceGrpcKt
import jp.momiji.grpc.momiji.brand.list.v1.brand
import jp.momiji.grpc.momiji.brand.list.v1.listBrandsResponse
import jp.momiji.util.toProtoTimestamp
import org.springframework.stereotype.Service

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
}
