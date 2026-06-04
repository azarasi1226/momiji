import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    idea

    kotlin("jvm") version "2.3.21"
    kotlin("plugin.spring") version "2.3.21"

    id("org.springframework.boot") version "4.0.6"
    id("io.spring.dependency-management") version "1.1.7"
    id("org.jooq.jooq-codegen-gradle") version "3.20.11"
    id("org.jlleitschuh.gradle.ktlint") version "14.0.1"
}

group = "momiji"
version = "0.0.1-SNAPSHOT"
description = "Identity Service Demo"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-jooq")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    implementation("org.springframework.boot:spring-boot-starter-mail")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    runtimeOnly("com.mysql:mysql-connector-j")

    // O11y
    implementation("org.springframework.boot:spring-boot-starter-opentelemetry")
    // logback のログを OTel SDK に橋渡しする appender。これが無いとログが OTLP export されず Loki に届かない。
    // バージョンは OTel コア (Spring Boot 4.0.6 管理の 1.55.0) に合わせる: instrumentation 2.21.0-alpha → core 1.55.0。
    implementation("io.opentelemetry.instrumentation:opentelemetry-logback-appender-1.0:2.21.0-alpha")
    // JDBC observation を提供するライブラリ。 これを入れると JDBC クエリが自動的に span 化される。
    implementation("net.ttddyy.observation:datasource-micrometer-spring-boot:2.2.1")
    // UseCaseLogicTracingAspect が `@Aspect` / `@Around` / `ProceedingJoinPoint` を使うため必要。
    // spring-aop 自体は spring-context の transitive で既に入っているが、 これらの annotation の
    // parsing には aspectjweaver が別途必要。
    implementation("org.aspectj:aspectjweaver")

    // テスト
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.testcontainers:testcontainers-junit-jupiter")
    testImplementation("org.testcontainers:testcontainers-mysql")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("com.ninja-squad:springmockk:5.0.1")

    // jooq
    implementation("org.jooq:jooq:3.20.11")
    jooqCodegen("com.mysql:mysql-connector-j")
    jooqCodegen("org.jooq:jooq-meta-extensions:3.20.11") // DDLDatabase用
    testImplementation("org.springframework.boot:spring-boot-starter-jooq-test")

    // Axon
    implementation(platform("io.axoniq.framework:axoniq-framework-bom:5.1.1"))
    implementation("org.axonframework.extensions.spring:axon-spring-boot-starter")
    implementation("io.axoniq.framework:axon-server-connector") // AxonFramework5.1からはAxonServerConnectorが別モジュールになったため、明示的に追加する必要がある。
    testImplementation("org.axonframework:axon-test")
    testImplementation("io.axoniq.framework:axoniq-testcontainer")

    // gRPC
    implementation("org.springframework.grpc:spring-grpc-spring-boot-starter:1.0.2")
    implementation("io.grpc:grpc-kotlin-stub:1.5.0")
    implementation("com.google.protobuf:protobuf-kotlin:4.34.1")

    // Other
    implementation("io.github.oshai:kotlin-logging-jvm:7.0.14") // ロギング
    implementation("de.huxhorn.sulky:de.huxhorn.sulky.ulid:8.3.0") // ULID生成
    implementation("software.amazon.awssdk:cognitoidentityprovider:2.42.8") // Cognitoクライアント
    implementation("software.amazon.awssdk:sso:2.42.8") // AWS_PROFILE が SSO プロファイルの場合に credentials を解決するために必要
    implementation("software.amazon.awssdk:ssooidc:2.42.8") // 同上
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core") // Coroutines (GRPC、Axon Command Gatewayで使用)
    implementation("com.michael-bull.kotlin-result:kotlin-result:2.0.1") // Result<V, E> 型 (値オブジェクトの validation 用)
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_25
        freeCompilerArgs.addAll(
            // Java側のNull性アノテーション（@NonNull/@Nullable等）をKotlinが「警告」ではなく「エラー」として扱うようにする。
            "-Xjsr305=strict",
            // data classのコンストラクタ引数に書いたアノテーションを、プロパティ側にも適用する。
            "-Xannotation-default-target=param-property",
            // data class の自動生成 copy() の visibility を primary constructor に揃える。
            // 例: internal constructor のとき copy() も internal に。
            // これにより、 値オブジェクトの validation を copy() でバイパスされる脆弱性を防ぐ。
            // 将来 Kotlin の default 挙動になる予定なので先取りしておく。
            "-Xconsistent-data-class-copy-visibility",
        )
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

// 自動生成コードの出力先（jOOQ / gRPC など）
val generatedSourcesDir = "build/generated-sources"
val jooqGeneratedDir = "$generatedSourcesDir/jooq"
val grpcGeneratedDir = "$generatedSourcesDir/grpc"

// 自動生成コードを main ソースセットの一部として認識させる
sourceSets.main {
    java.srcDirs(
        jooqGeneratedDir,
        grpcGeneratedDir,
    )
}

// =====================================================
// ========================jooq=========================
// =====================================================
jooq {
    configuration {
        generator {
            // Kotlin用のコードを生成
            name = "org.jooq.codegen.KotlinGenerator"
            database {
                // H2DBを利用し、atlasで管理しているschemaファイルを元にコード生成を行う
                name = "org.jooq.meta.extensions.ddl.DDLDatabase"
                properties {
                    property {
                        key = "scripts"
                        value = "./database/schema.mysql.sql"
                    }
                }
            }
            generate {
                // Null許容でないカラムに対してKotlinの非null型を使用する設定
                isKotlinNotNullRecordAttributes = true
            }
            target {
                packageName = "iss.jooq.generated"
                // 出力先を明示（sourceSetsで参照しているパスと一致させる）
                directory = jooqGeneratedDir
            }
        }
    }
}

// コンパイル前にjOOQのコード生成を実行するよう設定。これにより常に最新のスキーマに基づいたコードが生成される
// 幸いDDLDatabaseを使っているため、DBサーバーへの接続は発生しない完全ローカル完結...最高すぎかよ...
tasks.named("compileKotlin") {
    dependsOn("jooqCodegen")
}

// =====================================================
// ====================IntelliJ IDEA====================
// =====================================================
idea {
    module {
        // IntelliJ に「ここは自動生成コードだから干渉しないで」と伝える
        // → Find Usages / 検索 / インスペクション / リファクタの対象から外れる
        // 親ディレクトリだけ登録しておけば、配下に増えるgenerator（jOOQ/gRPC/...）も自動でカバーされる
        generatedSourceDirs.add(file(generatedSourcesDir))
    }
}

// =====================================================
// =======================ktlint========================
// =====================================================
// ktlintタスク（runKtlintCheckOverMainSourceSet 等）はsourceSetsを介して
// build/generated-sources/jooq を入力に持つため、jooqCodegen より後に走らせる必要がある。
// .editorconfigでlint対象のフォルダ絞っているのだが、Gradle 9.x の strict implicit-dependency 検出が有効になっていると、jooqCodegen → runKtlintCheckOverMainSourceSet の依存関係が明示されていないためにエラーになる。
tasks.matching { it.name.startsWith("runKtlint") }.configureEach {
    dependsOn("jooqCodegen")
}
