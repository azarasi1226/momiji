package jp.momiji.feature

import io.axoniq.framework.testcontainer.AxonServerContainerUtils
import iss.jooq.generated.tables.LookupExternalIdentities.Companion.LOOKUP_EXTERNAL_IDENTITIES
import iss.jooq.generated.tables.references.LOOKUP_EMAIL
import iss.jooq.generated.tables.references.USERS
import jp.momiji.DemoApplication
import org.axonframework.common.configuration.ApplicationConfigurer
import org.axonframework.test.fixture.AxonTestFixture
import org.axonframework.test.fixture.MessagesRecordingConfigurationEnhancer
import org.jooq.DSLContext
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource

/**
 * 統合テストの基底クラス。
 *
 * **ローカルdocker-compose と同等の環境** をTestContainersで再現する：
 * - MySQL（jOOQ Lookup用）
 * - Axon Server（DCB対応、CommandHandler/EventHandler の本物の流れ）
 * - Keycloak（実JWT発行/検証、KeycloakUserClient の管理API疎通）
 * - MailHog（SmtpMailSender が送るメールの受信サーバ。HTTP APIで取り出せる）
 *
 * EventStoreは @BeforeAll で1回purge、ReadDB側は @BeforeEach で全削除して、
 * テスト間の独立性を確保する。
 *
 * 個別のテストはこのクラスを継承し、`fixture` を使って
 * `given().events(...).when().command(...).then()` を組む。
 */
@SpringBootTest(classes = [DemoApplication::class])
@ActiveProfiles("integration-test")
@Import(MomijiIntegrationTestConfig::class)
abstract class MomijiIntegrationTestBase {
    @Autowired
    lateinit var configurer: ApplicationConfigurer

    @Autowired
    lateinit var dsl: DSLContext

    lateinit var fixture: AxonTestFixture

    companion object {
        // 全コンテナを起動時に並列で立ち上げる。接続情報は @DynamicPropertySource で一括登録。
        val mysql = TestContainerFactory.mysql().apply { start() }
        val axonServer = TestContainerFactory.axonServer().apply { start() }
        val keycloak = TestContainerFactory.keycloak().apply { start() }
        val mailhog = TestContainerFactory.mailhog().apply { start() }

        @JvmStatic
        @DynamicPropertySource
        fun registerProperties(registry: DynamicPropertyRegistry) {
            // MySQL（DataSource）
            registry.add("spring.datasource.url") { mysql.jdbcUrl }
            registry.add("spring.datasource.username") { mysql.username }
            registry.add("spring.datasource.password") { mysql.password }

            // Axon Server
            registry.add("axon.axonserver.servers") { axonServer.axonServerAddress }

            // Keycloak: realm "momiji" を test-realm.json から作成済み
            registry.add("momiji.oidc.issuer-uri") { "${keycloak.authServerUrl}/realms/momiji" }
            registry.add("momiji.keycloak.base-url") { keycloak.authServerUrl }
            registry.add("momiji.keycloak.realm") { "momiji" }
            registry.add("momiji.keycloak.admin-username") { keycloak.adminUsername }
            registry.add("momiji.keycloak.admin-password") { keycloak.adminPassword }

            // MailHog: SMTP受信サーバ
            registry.add("spring.mail.host") { mailhog.host }
            registry.add("spring.mail.port") { mailhog.getMappedPort(1025) }
        }

        @JvmStatic
        @BeforeAll
        fun beforeTestSuite() {
            // EventStoreの初期化（各テストクラスの開始時に1回）。
            // テスト間の独立性は ユニークな userId を使うことで担保しているが、
            // クラス境界では明示的にクリーンスレートにしておくことで再現性を上げる。
            AxonServerContainerUtils.purgeEventsFromAxonServer(
                axonServer.host,
                axonServer.httpPort,
                "default",
                AxonServerContainerUtils.DCB_CONTEXT,
            )
        }
    }

    @BeforeEach
    fun beforeTestCase() {
        // テスト間の独立性確保のため、Read DB側のテーブルを毎回空にする。
        // EventStore側はテスト毎にユニークな userId を使うことで衝突を回避している。
        dsl.deleteFrom(LOOKUP_EMAIL).execute()
        dsl.deleteFrom(LOOKUP_EXTERNAL_IDENTITIES).execute()
        dsl.deleteFrom(USERS).execute()

        fixture = AxonTestFixture.with(configurer)
    }

    @AfterEach
    fun afterTestCase() {
        fixture.stop()
    }
}

/**
 * 統合テスト用Bean設定。
 *
 * 全Beanを本番と同じものを使う（KeycloakUserClient / SmtpMailSender / NimbusJwtDecoder）。
 * 接続先だけがlocalのKeycloak/MailHogではなく TestContainer になる。
 *
 * [MessagesRecordingConfigurationEnhancer] はAxonTestFixture用に必要。
 */
@TestConfiguration
class MomijiIntegrationTestConfig {
    @Bean
    fun messagesRecordingConfigurationEnhancer() = MessagesRecordingConfigurationEnhancer()
}
