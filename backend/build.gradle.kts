plugins {
	kotlin("jvm") version "2.3.21"
	kotlin("plugin.spring") version "2.3.21"
	id("org.springframework.boot") version "4.0.6"
	id("io.spring.dependency-management") version "1.1.7"
	id("org.jooq.jooq-codegen-gradle") version "3.20.11"
}

group = "momiji"
version = "0.0.1-SNAPSHOT"
description = "Identity Service Demo"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

repositories {
	mavenCentral()
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-jooq")
	implementation("org.springframework.boot:spring-boot-starter-security")
	implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
	implementation("org.springframework.boot:spring-boot-starter-webmvc")
	implementation("org.springframework.boot:spring-boot-starter-mail")
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	runtimeOnly("com.mysql:mysql-connector-j")
	testImplementation("org.springframework.boot:spring-boot-starter-security-test")
	testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
	testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")

	// jooq
	testImplementation("org.springframework.boot:spring-boot-starter-jooq-test")
	implementation("org.jooq:jooq:3.20.11")
	jooqCodegen("com.mysql:mysql-connector-j")
	jooqCodegen("org.jooq:jooq-meta-extensions:3.20.11") // DDLDatabase用

	// Axon
	implementation(platform("org.axonframework:axon-framework-bom:5.1.1"))
	implementation("org.axonframework.extensions.spring:axon-spring-boot-starter")
	implementation("io.axoniq.framework:axon-server-connector:5.1.1")

	// gRPC
	implementation("org.springframework.grpc:spring-grpc-spring-boot-starter:1.0.2")
	implementation("io.grpc:grpc-kotlin-stub:1.5.0")
	implementation("com.google.protobuf:protobuf-kotlin:4.34.1")

	// Other
	implementation("io.github.oshai:kotlin-logging-jvm:7.0.14")
	implementation("de.huxhorn.sulky:de.huxhorn.sulky.ulid:8.3.0")
	implementation("software.amazon.awssdk:cognitoidentityprovider:2.42.8")
	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")

}

kotlin {
	compilerOptions {
		freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
	}
}

tasks.withType<Test> {
	useJUnitPlatform()
}

// =====================================================
// ======================jacoco=========================
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
			}
		}
	}
}

sourceSets.main {

	java.srcDirs(
		// jOOQの自働作成コード
		"build/generated-sources/jooq",
		// GRPCの自動作成コード
		"../grpc/gen/jvm")
}

// コンパイル前にjOOQのコード生成を実行するよう設定これにより常に最新のスキーマに基づいたコードが生成される
// 幸いDDLDatabaseを使っているため、DBサーバーへの接続は発生しない完全ローカル完結...最高すぎかよ...
tasks.named("compileKotlin") {
	dependsOn("jooqCodegen")
}
