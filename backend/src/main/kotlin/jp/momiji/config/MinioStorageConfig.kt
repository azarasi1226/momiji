package jp.momiji.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Configuration
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import java.net.URI

/**
 * ローカル開発用ストレージ（MinIO）の presigner。
 *
 * MinIO は単一ホストなので **パススタイル必須**、 creds は静的（minioadmin）。
 * 本番 S3 とは presigner の組み立てだけが違うので profile で分ける（[S3StorageConfig] と対）。
 * アダプタ自体は S3 プロトコル互換で共通（[jp.momiji.adapter.storage.S3ImageStorage]）。
 */
@Configuration
@Profile("storage-minio")
class MinioStorageConfig {
    @Bean
    fun s3Presigner(
        @Value("\${momiji.storage.region}") region: String,
        @Value("\${momiji.storage.endpoint}") endpoint: String,
        @Value("\${momiji.storage.access-key}") accessKey: String,
        @Value("\${momiji.storage.secret-key}") secretKey: String,
    ): S3Presigner =
        S3Presigner
            .builder()
            .region(Region.of(region))
            .endpointOverride(URI.create(endpoint))
            .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
            .credentialsProvider(
                StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)),
            ).build()
}
