package jp.momiji.feature.query.user.findmyprofile

import com.google.protobuf.timestamp
import jp.momiji.config.grpc.GrpcAuthContext
import jp.momiji.domain.BusinessError
import jp.momiji.domain.BusinessException
import jp.momiji.feature.command.UserIdResolver
import jp.momiji.grpc.momiji.user.findmyprofile.FindMyProfileRequest
import jp.momiji.grpc.momiji.user.findmyprofile.FindMyProfileResponse
import jp.momiji.grpc.momiji.user.findmyprofile.FindMyProfileServiceGrpcKt
import jp.momiji.grpc.momiji.user.findmyprofile.findMyProfileResponse
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.ZoneOffset

@Service
class FindMyProfileGrpcService(
    private val userIdResolver: UserIdResolver,
    private val findMyProfileQueryService: FindMyProfileQueryService,
) : FindMyProfileServiceGrpcKt.FindMyProfileServiceCoroutineImplBase() {
    override suspend fun findMyProfile(request: FindMyProfileRequest): FindMyProfileResponse {
        val accessToken = GrpcAuthContext.current().token
        val userId = userIdResolver.resolve(accessToken)

        val user =
            findMyProfileQueryService.find(userId)
                ?: throw BusinessException(BusinessError("ユーザーが見つかりません"))

        return findMyProfileResponse {
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
