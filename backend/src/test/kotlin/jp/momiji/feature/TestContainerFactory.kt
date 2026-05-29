package jp.momiji.feature

import dasniko.testcontainers.keycloak.KeycloakContainer
import io.axoniq.framework.testcontainer.AxonServerContainer
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.mysql.MySQLContainer
import org.testcontainers.utility.MountableFile
import java.nio.file.Paths

/**
 * 統合テスト用のTestContainerファクトリ。ローカル docker-compose と同じ構成 (MySQL / Axon Server /
 * Keycloak / MailHog) をテスト中に立ち上げる。
 *
 * - [mysql]: スキーマ初期化済みのMySQL 8.0
 * - [axonServer]: DCB対応 + DevMode（EventStore全削除APIを使えるように）
 * - [keycloak]: realm "momiji" インポート済み。実JWT発行/検証フローが流れる
 * - [mailhog]: SMTP受信用。EmailChangeEmailSender が送ったメールはここに溜まり、HTTP APIで取り出せる
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

    fun keycloak(): KeycloakContainer =
        // バージョン指定なしのデフォルトコンストラクタで、testcontainers-keycloakライブラリが
        // 検証済みのKeycloakイメージを使用する
        KeycloakContainer()
            // src/test/resources/test-realm.json を起動時にインポート
            .withRealmImportFile("test-realm.json")

    /**
     * MailHog SMTP受信サーバ。
     * - 1025: SMTP（Spring Bootの SmtpMailSender からの送信先）
     * - 8025: HTTP UI / API（送信されたメールを取り出すのに使う）
     */
    fun mailhog(): GenericContainer<*> =
        // GenericContainer<Nothing> の流暢チェーンは F-bounded generic の制約で型推論が失敗する
        // （withExposedPorts の戻り値型が Nothing と解決され、後続メソッドが呼べない）。
        // よって apply { } で this を介して各設定メソッドを呼ぶ
        GenericContainer<Nothing>("mailhog/mailhog:latest").apply {
            withExposedPorts(1025, 8025)
            waitingFor(Wait.forListeningPort())
        }
}
