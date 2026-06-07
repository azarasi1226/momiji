package jp.momiji.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.presigner.S3Presigner

/**
 * 本番/テスト用ストレージ（実 S3）の presigner。
 *
 * creds は [DefaultCredentialsProvider]（IAM ロール等。 **secret を設定に持たない**）、
 * アドレッシングは S3 既定の virtual-hosted（パススタイルは指定しない）。 CognitoConfig と同じ思想。
 * ローカル MinIO とは presigner の組み立てだけが違うので profile で分ける（[MinioStorageConfig] と対）。
 */
@Configuration
@Profile("storage-s3")
class S3StorageConfig {
    @Bean
    fun s3Presigner(
        @Value("\${momiji.storage.region}") region: String,
    ): S3Presigner =
        S3Presigner
            .builder()
            .region(Region.of(region))
            .credentialsProvider(DefaultCredentialsProvider.builder().build())
            .build()
}
