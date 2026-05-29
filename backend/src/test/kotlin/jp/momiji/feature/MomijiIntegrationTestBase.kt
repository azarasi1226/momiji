package jp.momiji.feature

import com.ninjasquad.springmockk.MockkSpyBean
import iss.jooq.generated.tables.LookupExternalIdentities.Companion.LOOKUP_EXTERNAL_IDENTITIES
import iss.jooq.generated.tables.references.LOOKUP_EMAIL
import iss.jooq.generated.tables.references.USERS
import jp.momiji.DemoApplication
import jp.momiji.feature.idp.IdpUserClient
import jp.momiji.feature.mail.MailSender
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.axonframework.common.configuration.ApplicationConfigurer
import org.axonframework.test.fixture.AxonTestFixture
import org.axonframework.test.fixture.MessagesRecordingConfigurationEnhancer
import org.jooq.DSLContext
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.lifecycle.Startable

/**
 * 統合テストの基底クラス。
 *
 * **ローカルdocker-compose と同等の環境** をTestContainersで再現する：
 * - MySQL（jOOQ Lookup用）
 * - Axon Server（DCB対応、CommandHandler/EventHandler の本物の流れ）
 * - Keycloak（実JWT発行/検証、KeycloakUserClient の管理API疎通）
 * - MailHog（SmtpMailSender が送るメールの受信サーバ。HTTP APIで取り出せる）
 *
 * テスト間の独立性は ReadDB側を @BeforeEach で全削除して確保する。
 * EventStore側は各テストでユニークな userId（ULID 風の固定文字列）を使うことで衝突を回避している。
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

    /**
     * 副作用検証用の MockK spy（springmockk）。実装そのまま（SmtpMailSender → MailHog、
     * KeycloakUserClient → Keycloak）をラップしているので、実 IO は流れたうえで
     * `verify { ... }` で呼び出し検証ができる。
     *
     * 注意: spy 宣言は **必ずこの基底クラスに集約する**。 test class ごとに違う組合せを宣言すると
     * Spring の TestContext cache が別 fingerprint と判断して context を restart() しようとし、
     * Axon の start lifecycle handler が二重実行されて RepositoryAlreadyRegisteredException で落ちる。
     *
     * spy の interaction 履歴は springmockk が各テスト後に自動 clear するので明示 clear 不要。
     */
    @MockkSpyBean
    lateinit var mailSender: MailSender

    @MockkSpyBean
    lateinit var idpUserClient: IdpUserClient

    lateinit var fixture: AxonTestFixture

    companion object {
        // 全コンテナを並列で立ち上げる。接続情報は @DynamicPropertySource で一括登録。
        // 直列 apply { start() } だと 4コンテナぶんの起動時間が直列加算されてしまうため、
        // 各コンテナは独立で IO 待ちが支配的なので Dispatchers.IO の async で同時起動する。
        val mysql = TestContainerFactory.mysql()
        val axonServer = TestContainerFactory.axonServer()
        val keycloak = TestContainerFactory.keycloak()
        val mailhog = TestContainerFactory.mailhog()

        init {
            runBlocking {
                listOf<Startable>(mysql, axonServer, keycloak, mailhog)
                    .map { container -> async(Dispatchers.IO) { container.start() } }
                    .awaitAll()
            }
        }

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
