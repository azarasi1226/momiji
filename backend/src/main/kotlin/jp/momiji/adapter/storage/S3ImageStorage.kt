package jp.momiji.adapter.storage

import de.huxhorn.sulky.ulid.ULID
import jp.momiji.port.storage.ImageContentType
import jp.momiji.port.storage.ImageStorage
import jp.momiji.port.storage.UploadTarget
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import java.time.Duration

/**
 * S3（互換: local は MinIO）への画像アップロードを presigned PUT で行うアダプタ。
 *
 * - object key は `images/<ULID>.<ext>`（衝突回避・キャッシュ事故防止）
 */
@Component
@Profile("storage-minio | storage-s3")
class S3ImageStorage(
    private val s3Presigner: S3Presigner,
    @Value("\${momiji.storage.bucket}") private val bucket: String,
    @Value("\${momiji.storage.public-base-url}") private val publicBaseUrl: String,
) : ImageStorage {
    private val ulid = ULID()

    override fun issueUploadUrl(contentType: ImageContentType): UploadTarget {
        val key = "images/${ulid.nextULID()}.${contentType.extension}"

        val presigned =
            s3Presigner.presignPutObject { builder ->
                builder.signatureDuration(Duration.ofMinutes(UPLOAD_URL_EXPIRY_MINUTES))
                builder.putObjectRequest(
                    PutObjectRequest
                        .builder()
                        .bucket(bucket)
                        .key(key)
                        .contentType(contentType.mime)
                        .build(),
                )
            }

        return UploadTarget(
            uploadUrl = presigned.url().toString(),
            publicUrl = "${publicBaseUrl.trimEnd('/')}/$key",
        )
    }

    private companion object {
        const val UPLOAD_URL_EXPIRY_MINUTES = 5L
    }
}
