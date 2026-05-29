package jp.momiji

import io.axoniq.framework.testcontainer.AxonServerContainer
import org.testcontainers.mysql.MySQLContainer
import org.testcontainers.utility.MountableFile
import java.nio.file.Paths

/**
 * 統合テスト用の TestContainer ファクトリ。 CQRS+ES のコアパス検証に必要な
 * MySQL (jOOQ Lookup) と Axon Server (DCB EventStore) だけを提供する。
 *
 * IDP / Mail / JwtDecoder / OidcUserInfoFetcher は @MockkBean で置き換えるので
 * Keycloak / MailHog のコンテナは立てない (MomijiIntegrationTestBase 参照)。
 */
object TestContainerFactory {
    fun mysql(): MySQLContainer =
        MySQLContainer("mysql:8.0").apply {
            // backend/ をカレントとした相対パスで schema を取得し、コンテナの
            // /docker-entrypoint-initdb.d/ に配置することで起動時に自動適用させる
            val schemaPath =
                Paths
                    .get("database/schema.mysql.sql")
                    .toAbsolutePath()
            withCopyFileToContainer(
                MountableFile.forHostPath(schemaPath),
                "/docker-entrypoint-initdb.d/schema.mysql.sql",
            )
        }

    fun axonServer(): AxonServerContainer =
        AxonServerContainer()
            .withAxonServerHostname("localhost")
            // DCB（Dynamic Consistency Boundary）機能を有効化
            .withDcbContext(true)
            // AxonServerContainerUtils.purgeEventsFromAxonServer を呼ぶために必要
            .withDevMode(true)
}
