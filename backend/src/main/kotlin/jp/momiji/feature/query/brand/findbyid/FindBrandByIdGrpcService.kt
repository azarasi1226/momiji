package jp.momiji.feature.query.brand.findbyid

import jp.momiji.domain.BusinessError
import jp.momiji.domain.BusinessException
import jp.momiji.feature.command.brand.brandStatusToProto
import jp.momiji.grpc.momiji.brand.findbyid.v1.FindBrandByIdRequest
import jp.momiji.grpc.momiji.brand.findbyid.v1.FindBrandByIdResponse
import jp.momiji.grpc.momiji.brand.findbyid.v1.FindBrandByIdServiceGrpcKt
import jp.momiji.grpc.momiji.brand.findbyid.v1.findBrandByIdResponse
import jp.momiji.util.toProtoTimestamp
import org.springframework.stereotype.Service

@Service
class FindBrandByIdGrpcService(
    private val findBrandByIdQueryService: FindBrandByIdQueryService,
) : FindBrandByIdServiceGrpcKt.FindBrandByIdServiceCoroutineImplBase() {
    override suspend fun findBrandById(request: FindBrandByIdRequest): FindBrandByIdResponse {
        val brand =
            findBrandByIdQueryService.findById(request.id)
                ?: throw BusinessException(BusinessError("ブランドが見つかりません"))

        return findBrandByIdResponse {
            id = brand.id
            name = brand.name
            description = brand.description
            status = brandStatusToProto(brand.status)
            createdAt = brand.createdAt.toProtoTimestamp()
            updatedAt = brand.updatedAt.toProtoTimestamp()
        }
    }
}
