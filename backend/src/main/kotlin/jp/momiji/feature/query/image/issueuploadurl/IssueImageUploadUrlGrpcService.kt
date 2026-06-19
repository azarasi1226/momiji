package jp.momiji.feature.query.image.issueuploadurl

import com.github.michaelbull.result.getOrElse
import jp.momiji.domain.ValidationException
import jp.momiji.grpc.momiji.image.upload.IssueImageUploadUrlRequest
import jp.momiji.grpc.momiji.image.upload.IssueImageUploadUrlResponse
import jp.momiji.grpc.momiji.image.upload.IssueImageUploadUrlServiceGrpcKt
import jp.momiji.grpc.momiji.image.upload.issueImageUploadUrlResponse
import jp.momiji.port.storage.ImageContentType
import jp.momiji.port.storage.ImageStorage
import org.springframework.stereotype.Service

/**
 * 画像アップロード用 presigned PUT URL を発行する gRPC サービス。
 *
 * 状態を変えず（イベントも DB 書き込みも無い）データ（URL）を返すだけなので query 側に置く
 */
@Service
class IssueImageUploadUrlGrpcService(
    private val imageStorage: ImageStorage,
) : IssueImageUploadUrlServiceGrpcKt.IssueImageUploadUrlServiceCoroutineImplBase() {
    override suspend fun issueImageUploadUrl(request: IssueImageUploadUrlRequest): IssueImageUploadUrlResponse {
        val contentType =
            ImageContentType
                .fromMime(request.contentType)
                .getOrElse { error -> throw ValidationException(listOf(error)) }

        val target = imageStorage.issueUploadUrl(contentType)

        return issueImageUploadUrlResponse {
            uploadUrl = target.uploadUrl
            publicUrl = target.publicUrl
        }
    }
}
