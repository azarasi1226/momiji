package jp.momiji.feature.user.findbyid

import com.google.protobuf.timestamp
import jp.momiji.feature.Error
import jp.momiji.feature.UseCaseException
import jp.momiji.feature.user.UserIdResolver
import jp.momiji.grpc.GrpcAuthContext
import jp.momiji.grpc.momiji.user.findbyid.v1.FindUserByIdRequest
import jp.momiji.grpc.momiji.user.findbyid.v1.FindUserByIdResponse
import jp.momiji.grpc.momiji.user.findbyid.v1.FindUserByIdServiceGrpcKt
import jp.momiji.grpc.momiji.user.findbyid.v1.findUserByIdResponse
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.ZoneOffset

@Service
class FindUserByIdGrpcService(
  private val userIdResolver: UserIdResolver,
  private val findUserByIdQueryService: FindUserByIdQueryService,
) : FindUserByIdServiceGrpcKt.FindUserByIdServiceCoroutineImplBase() {

  override suspend fun findUserById(request: FindUserByIdRequest): FindUserByIdResponse {
    val auth = GrpcAuthContext.current()
    val userId = userIdResolver.resolve(auth)

    val user = findUserByIdQueryService.findById(userId)
      ?: throw UseCaseException(Error("ユーザーが見つかりません"))

    return findUserByIdResponse {
      id = user.id
      email = user.email
      name = user.name
      phoneNumber = user.phoneNumber
      postalCode = user.postalCode
      address1 = user.address1
      address2 = user.address2
      createdAt = user.createdAt.toProtoTimestamp()
      updatedAt = user.updatedAt.toProtoTimestamp()
    }
  }

  private fun LocalDateTime.toProtoTimestamp() = timestamp {
    val instant = this@toProtoTimestamp.toInstant(ZoneOffset.UTC)
    seconds = instant.epochSecond
    nanos = instant.nano
  }
}
