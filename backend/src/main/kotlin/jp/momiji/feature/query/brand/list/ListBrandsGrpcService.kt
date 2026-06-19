package jp.momiji.feature.query.brand.list

import jp.momiji.feature.command.brand.brandStatusToProto
import jp.momiji.grpc.momiji.brand.list.ListBrandsRequest
import jp.momiji.grpc.momiji.brand.list.ListBrandsResponse
import jp.momiji.grpc.momiji.brand.list.ListBrandsServiceGrpcKt
import jp.momiji.grpc.momiji.brand.list.brand
import jp.momiji.grpc.momiji.brand.list.listBrandsResponse
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
