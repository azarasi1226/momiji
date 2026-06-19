package jp.momiji.feature.query.user.findbyid

import com.google.protobuf.timestamp
import jp.momiji.config.grpc.GrpcAuthContext
import jp.momiji.domain.BusinessError
import jp.momiji.domain.BusinessException
import jp.momiji.feature.command.UserIdResolver
import jp.momiji.grpc.momiji.user.findbyid.FindUserByIdRequest
import jp.momiji.grpc.momiji.user.findbyid.FindUserByIdResponse
import jp.momiji.grpc.momiji.user.findbyid.FindUserByIdServiceGrpcKt
import jp.momiji.grpc.momiji.user.findbyid.findUserByIdResponse
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.ZoneOffset

@Service
class FindUserByIdGrpcService(
    private val userIdResolver: UserIdResolver,
    private val findUserByIdQueryService: FindUserByIdQueryService,
) : FindUserByIdServiceGrpcKt.FindUserByIdServiceCoroutineImplBase() {
    override suspend fun findUserById(request: FindUserByIdRequest): FindUserByIdResponse {
        val accessToken = GrpcAuthContext.current().token
        val userId = userIdResolver.resolve(accessToken)

        val user =
            findUserByIdQueryService.findById(userId)
                ?: throw BusinessException(BusinessError("ユーザーが見つかりません"))

        return findUserByIdResponse {
            id = user.id
            email = user.email
            name = user.name
            createdAt = user.createdAt.toProtoTimestamp()
            updatedAt = user.updatedAt.toProtoTimestamp()
        }
    }

    private fun LocalDateTime.toProtoTimestamp() =
        timestamp {
            val instant = this@toProtoTimestamp.toInstant(ZoneOffset.UTC)
            seconds = instant.epochSecond
            nanos = instant.nano
        }
}
