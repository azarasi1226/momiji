plugins {
	kotlin("jvm") version "2.2.21"
	kotlin("plugin.spring") version "2.2.21"
	id("org.springframework.boot") version "4.0.3"
	id("io.spring.dependency-management") version "1.1.7"
	id("org.jooq.jooq-codegen-gradle") version "3.20.11"
}

group = "momiji"
version = "0.0.1-SNAPSHOT"
description = "Identity Service Demo"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(24)
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
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("tools.jackson.module:jackson-module-kotlin")
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

	// Other
	implementation("org.axonframework.extensions.spring:axon-spring-boot-starter:5.0.3")
	implementation("io.github.oshai:kotlin-logging-jvm:7.0.14")
	implementation("de.huxhorn.sulky:de.huxhorn.sulky.ulid:8.3.0")
	implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.0.1")
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
	// jOOQのコード生成先をGradleの生成ソースディレクトリに設定
	// これにより、自動生成されたコードがコンパイル対象に含まれるようになる
	java.srcDirs("build/generated-sources/jooq")
}

// コンパイル前にjOOQのコード生成を実行するよう設定これにより常に最新のスキーマに基づいたコードが生成される
// 幸いDDLDatabaseを使っているため、DBサーバーへの接続は発生しない完全ローカル完結...最高すぎかよ...
tasks.named("compileKotlin") {
	dependsOn("jooqCodegen")
}
