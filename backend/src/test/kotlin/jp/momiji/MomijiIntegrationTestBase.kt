package jp.momiji

import com.ninjasquad.springmockk.MockkBean
import iss.jooq.generated.tables.LookupExternalIdentities.Companion.LOOKUP_EXTERNAL_IDENTITIES
import iss.jooq.generated.tables.references.LOOKUP_EMAIL
import iss.jooq.generated.tables.references.USERS
import jp.momiji.feature.idp.IdpUserClient
import jp.momiji.feature.mail.MailSender
import jp.momiji.feature.user.create.OidcUserInfoFetcher
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
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.lifecycle.Startable

/**
 * 統合テストの基底クラス。
 *
 * CQRS+ES のコアパスを検証するための最小構成:
 * - MySQL TestContainer（jOOQ Lookup テーブル操作の本物の流れ）
 * - Axon Server TestContainer（DCB EventStore、 CommandHandler / EventHandler の本物の流れ）
 *
 * 外部 IO の bean は **mock 化** することでコンテナと profile を削減している:
 * - [MailSender] / [IdpUserClient] / [OidcUserInfoFetcher] / [JwtDecoder]
 * - これらは「呼ばれたこと」を verify する目的のテストにしか使わない
 * - 実際の SMTP 配送 / Keycloak admin API / JWT 検証 / OIDC discovery は production smoke test で確認する責務
 *
 * テスト間の独立性は ReadDB 側を @BeforeEach で全削除して確保する。
 * EventStore 側は各テストでユニークな userId（ULID 風の固定文字列）を使うことで衝突を回避している。
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
     * 副作用検証用の MockK mock（springmockk）。 副作用ハンドラ (EventHandler) が
     * 期待するメソッド呼び出しを行ったかを `verify { ... }` で確認する。
     *
     * 注意: mock 宣言は **必ずこの基底クラスに集約する**。 test class ごとに違う組合せを宣言すると
     * Spring の TestContext cache が別 fingerprint と判断して context を restart() しようとし、
     * Axon の start lifecycle handler が二重実行されて RepositoryAlreadyRegisteredException で落ちる。
     *
     * mock の interaction 履歴は springmockk が各テスト後に自動 clear するので明示 clear 不要。
     */
    @MockkBean(relaxed = true)
    lateinit var mailSender: MailSender

    @MockkBean(relaxed = true)
    lateinit var idpUserClient: IdpUserClient

    /**
     * 統合テストでは gRPC 入口経由のテストはしないので JwtDecoder は呼ばれないが、
     * Spring context 起動時に [jp.momiji.config.SecurityConfig] が
     * `NimbusJwtDecoder.withIssuerLocation(issuerUri).build()` で OIDC discovery を叩こうとして
     * Keycloak がいないと死ぬ。 mock で bean factory を skip する。
     */
    @MockkBean(relaxed = true)
    lateinit var jwtDecoder: JwtDecoder

    /**
     * 統合テストでは CreateUserCommandHandler を直接叩くので OidcUserInfoFetcher は呼ばれないが、
     * Spring component の init ブロックで OIDC discovery を叩こうとして Keycloak がいないと死ぬ。
     * mock で bean factory を skip する。
     */
    @MockkBean(relaxed = true)
    lateinit var oidcUserInfoFetcher: OidcUserInfoFetcher

    lateinit var fixture: AxonTestFixture

    companion object {
        // コンテナを並列で起動
        val mysql = TestContainerFactory.mysql()
        val axonServer = TestContainerFactory.axonServer()

        init {
            runBlocking {
                listOf<Startable>(mysql, axonServer)
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
 * 統合テスト用 Bean 設定。
 *
 * [MessagesRecordingConfigurationEnhancer] は AxonTestFixture 用に必要。
 */
@TestConfiguration
class MomijiIntegrationTestConfig {
    @Bean
    fun messagesRecordingConfigurationEnhancer() = MessagesRecordingConfigurationEnhancer()
}
