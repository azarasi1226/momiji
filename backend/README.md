# Momiji Backend

## アーキテクチャ

### CQRS ( Command Query Responsibility Segregation )

書き込みと読み込みを別データストアに分離するパターン。 書き込み側は整合性を厳密に守り、 読み込み側は表示 / 検索用にスキーマ・インデックスを最適化できる。  
momiji では書き込みを Axon の Command 経由で処理しイベントとして AxonServer に保存、 読み込み側は イベントをMySQL にプロジェクションしてリードモデルを構築している。

### Event Sourcing

Git のように、過去のイベントをもとに現在の状態を作るパターン。 「現状」 をそのまま保存せず、 「何が起きたか」 のイベント列だけを永続化して、 必要な時に再生して状態を組み立てる。  
副産物として監査ログがタダで手に入る + 任意時点に巻き戻せる + 将来 Read model を追加したくなっても過去イベント再生で初期化できる。

### DCB ( Dynamic Consistency Boundary )

Axon Framework 5 の新機能。 従来の DDD では Aggregate という整合性境界を予め設計しておく必要があったが、 DCB は **Command ごとに** 「このコマンドが触る Event タグの集合」 を動的に整合性境界として扱う。  
結果として Aggregate を事前設計する必要がなくなり、 集約をまたぐ整合性チェックも自然に書けるようになり、 細粒度な並列性も出る。

### 垂直スライス アーキテクチャ ( Vertical Slice )

レイヤー ( Controller / UseCase / Infra ) ではなく **ユースケース単位** でフォルダを切る方式。  
例えば「ユーザー更新」 に関する `XxxGrpcService` / `XxxCommandHandler` / `XxxEventHandler` / `XxxCommand` は全部 `feature/command/user/update/` に同居。 機能追加 / 削除時の影響範囲がそのフォルダに閉じる。  
DCB との相性が良く、 集約という共通の概念がない分、 ユースケースごとに必要なコードを簡潔にまとめやすい。

### DDD (一部)

このプロジェクトでは **値オブジェクトだけ** を取り入れている。  
集約もRepositoryも存在しない。

---

## 目次

1. [技術スタック](#技術スタック)
2. [ディレクトリ構造](#ディレクトリ構造)
3. [起動と開発フロー](#起動と開発フロー)
4. [プロファイルと環境変数](#プロファイルと環境変数)
5. [gRPC まわり](#grpc-まわり)
6. [自動生成コード](#自動生成コード)
7. [ビルド設定の要点](#ビルド設定の要点)
8. [TODO](#todo)

---

## バックエンド技術スタック

| 項目                   | 採用                                                      |
|----------------------|---------------------------------------------------------|
| 言語                   | Kotlin                                                  |
| ランタイム                | Java 25                                                 |
| アプリケーション             | Spring Boot 4系                                          |
| API                  | gRPC                                                    |
| Event Store / CQRS基盤 | Axon Framework 5.1.1 + Axon Server                      |
| ORM                  | jOOQ                                                    |
| DB                   | MySQL                                                   |
| DBスキーマ管理             | Atlas（`database/schema.mysql.sql` をマスタとし、jOOQ がそれを読んでコード生成） |
| 認証                   | OIDC（Keycloak / Cognito 等）+ JWT                         |
| オブザービリティ             | OpenTelemetory                                          |
| Lint                 | ktlint（Gradleプラグイン）+ `.editorconfig`                    |
| カバレッジ                | Kover（Gradleプラグイン、`koverHtmlReport` / `koverXmlReport`）   |

---

## ディレクトリ構造

```
backend/src/main/kotlin/jp/momiji/
├─ DemoApplication.kt    … Spring Boot エントリポイント
├─ domain/               … ドメインオブジェクト（値オブジェクト・エラー型・idp ドメイン）
├─ event/                … ドメインイベント（@Event。 書き込み/読み取り両側の契約）
├─ feature/              … アプリケーション層。 CQRS で command / query に分け、 配下は1ユースケース1パッケージ（vertical slice）
│    ├─ command/         … 書き込み（CommandHandler + GrpcService + そのユースケース内の副作用handler / lookup projector）
│    └─ query/           … 読み取り（QueryService + GrpcService）
├─ projection/           … Read model 構築（Projector。 イベントを購読して read テーブルを更新）
├─ port/                 … 外向きポート（interface：idp / mail）。 横断的な外部契約
├─ adapter/              … 外向きポートの実装（idp: Cognito/Keycloak、mail: Smtp）
├─ config/               … 設定群（Axon / jOOQ / Security / gRPC / 観測の Aspect・Sampler）
└─ util/                 … 横断ユーティリティ（zipOrAccumulate 拡張、 LocalDateTime→proto Timestamp 変換 等）
```

## 環境変数

application.yaml では `${...}` プレースホルダで値を参照しており、 起動時に解決される。 解決元は環境によって違う:

- **ローカル開発**: [local.env.properties](src/main/resources/local.env.properties) ( resources/ 配下に commit 済み ) が `application-app-local.yaml` の `spring.config.import` 経由で読み込まれて値を提供する。 開発者が自分で env var をセットする必要は無い。 例外は秘密値 ( Stripe キー等 ) で、 gitignore 対象の `local.secret.properties` ( 各自作成 ) に置く。
- **test / prod**: 環境変数 ( k8s の Secret / ConfigMap や ECS の task definition 等で渡す想定 ) から解決される。

**必須** のものが未設定だと yaml の `${...}` が解決できず **起動時例外で落ちる** ( fail-fast )

| 環境変数 | 必須 | 定義元 yaml | 説明 |
|---|---|---|---|
| `SERVER_PORT` | ❌ ( default: `9090` ) | [app-common](src/main/resources/application-app-common.yaml) | HTTP server port ( Stripe webhook の受け口 `/api/webhooks/stripe` が載る ) |
| `GRPC_PORT` | ❌ ( default: `9091` ) | [app-common](src/main/resources/application-app-common.yaml) | gRPC server port |
| `APPLICATION_NAME` | ❌ ( default: `momiji-backend` ) | [app-common](src/main/resources/application-app-common.yaml) | アプリケーション名 ( メトリクス / トレースの `service.name` にも使われる ) |
| `JWT_ISSUER_URI` | ✅ | [app-common](src/main/resources/application-app-common.yaml) | OIDC issuer URI ( Cognito なら `https://cognito-idp.{region}.amazonaws.com/{poolId}` ) |
| `OIDC_CLIENT_ID` | ✅ | [app-common](src/main/resources/application-app-common.yaml) | このトークンが宛てられるべきクライアントID。 frontend(BFF) の client id と一致させる ( local Keycloak=`momiji` / 本番 Cognito=app client id )。 access token の `azp`(Keycloak) / `client_id`(Cognito) と照合する ([JwtClientIdValidator](src/main/kotlin/jp/momiji/config/grpc/JwtClientIdValidator.kt)) |
| `EMAIL_CHANGE_SECRET` | ✅ | [app-common](src/main/resources/application-app-common.yaml) | メール変更トークン署名鍵 ( **HS256 のため 32byte 以上必須**、 [EmailChangeTokenService](src/main/kotlin/jp/momiji/feature/command/user/changeemail/EmailChangeTokenService.kt) で長さ検証あり ) |
| `MAIL_FROM` | ✅ | [app-common](src/main/resources/application-app-common.yaml) | メール送信元アドレス |
| `COGNITO_USER_POOL_ID` | ✅ ( idp-cognito 時 ) | [idp-cognito](src/main/resources/application-idp-cognito.yaml) | Cognito User Pool ID |
| `AWS_REGION` | ❌ ( default: `ap-northeast-1` ) | [idp-cognito](src/main/resources/application-idp-cognito.yaml) / [storage-s3](src/main/resources/application-storage-s3.yaml) | AWS リージョン ( Cognito / S3 presigner 共通 ) |
| `SPRING_DATASOURCE_HOST` | ✅ | [datastore-mysql](src/main/resources/application-datastore-mysql.yaml) | MySQL host |
| `SPRING_DATASOURCE_PORT` | ✅ | [datastore-mysql](src/main/resources/application-datastore-mysql.yaml) | MySQL port |
| `SPRING_DATASOURCE_DATABASE` | ✅ | [datastore-mysql](src/main/resources/application-datastore-mysql.yaml) | MySQL database name |
| `SPRING_DATASOURCE_USERNAME` | ✅ | [datastore-mysql](src/main/resources/application-datastore-mysql.yaml) | MySQL username |
| `SPRING_DATASOURCE_PASSWORD` | ✅ | [datastore-mysql](src/main/resources/application-datastore-mysql.yaml) | MySQL password |
| `STORAGE_BUCKET` | ✅ ( storage-s3 時 ) | [storage-s3](src/main/resources/application-storage-s3.yaml) | 画像を置く S3 バケット名 |
| `STORAGE_PUBLIC_BASE_URL` | ✅ ( storage-s3 時 ) | [storage-s3](src/main/resources/application-storage-s3.yaml) | 画像の恒久・公開 URL の基準 ( CloudFront or 公開 S3 )。 末尾に `/<key>` が付く。 DB の `image_url` にこの URL を保存する |
| `OTLP_METRICS_URL` | ✅ | [observability-otlp](src/main/resources/application-observability-otlp.yaml) | メトリクスの OTLP push 先 URL ( 例: `http://otel-collector:4318/v1/metrics` ) |
| `OTLP_TRACES_ENDPOINT` | ✅ | [observability-otlp](src/main/resources/application-observability-otlp.yaml) | トレースの OTLP push 先 URL ( 例: `http://tempo:4318/v1/traces` ) |
| `TRACE_SAMPLING_PROBABILITY` | ✅ | [observability-otlp](src/main/resources/application-observability-otlp.yaml) | トレース sampling 比率 ( 0.0 〜 1.0、 本番 0.1 程度推奨 ) |
| `STRIPE_SECRET_KEY` | ✅ ( default: `sk_test_dummy` ) | [payment-stripe](src/main/resources/application-payment-stripe.yaml) | Stripe シークレットキー ( `sk_test_` / `sk_live_` )。 default はダミーのため未設定でも起動はするが、 カード登録系の操作時に失敗する。 ローカルは `local.secret.properties` に置く |
| `STRIPE_WEBHOOK_SECRET` | ✅ ( default: `whsec_dummy` ) | [payment-stripe](src/main/resources/application-payment-stripe.yaml) | webhook 署名検証シークレット。 ローカルは `stripe listen` が払い出す `whsec_` を使う ( 不一致だと webhook が 400 ) |

## gRPC まわりのお作法
### 非認証エンドポイントを作りたい

特定のメソッドだけ認証を外したいときは [@PublicEndpoint](src/main/kotlin/jp/momiji/config/grpc/PublicEndpoint.kt) をメソッドに付ける。

```kotlin
@Service
class HealthGrpcService : HealthGrpcKt.HealthCoroutineImplBase() {
    @PublicEndpoint                       
    override suspend fun check(...) { ... }
}
```

### 認証情報の取り出し — `GrpcAuthContext`

認証はInterceptorで強制されるが、**「誰のリクエストか」を知りたい時だけ**(アクセストークン) Context から取り出す：

```kotlin
override suspend fun createUser(request: CreateUserRequest): CreateUserResponse {
    val auth = GrpcAuthContext.current()           // ← JwtAuthenticationToken
    val accessToken = auth.token.tokenValue
    // ...
}
```

### 例外ハンドリング

[GrpcConfig](src/main/kotlin/jp/momiji/config/grpc/GrpcConfig.kt) の `grpcExceptionHandler` で `BusinessException` → `Status.INVALID_ARGUMENT` に変換している。

---

## 自動生成コード一覧

両方とも `backend/build/generated-sources/` 配下に出力される。`build/` は gitignored なのでコミットされない。

| 種類 | 生成元 | 出力先 |
|---|---|---|
| jOOQ | `database/schema.mysql.sql` | `build/generated-sources/jooq/iss/jooq/generated/` |
| gRPC | `grpc/*.proto`（`buf generate`） | `build/generated-sources/grpc/` |

- `jooqCodegen` タスクは `compileKotlin` の前に自動実行される
- gRPC は **buf を別途実行** する必要がある（Gradleでは生成されない）

---

## ビルド設定の要点

[build.gradle.kts](build.gradle.kts) の中でも特殊な部分：

### `-Xjsr305=strict`
Java側のNull性アノテーション（Spring等）をKotlin側でエラー扱い。**実行時NPE防止のため必須**。

### `-Xannotation-default-target=param-property`
`data class` のコンストラクタ引数に書いたアノテーションをプロパティ側にも適用する。`@TargetEntityId` のような Axon アノテーションが正しく検出されるために**必須**。

### `tasks.matching { it.name.startsWith("runKtlint") }.configureEach { dependsOn("jooqCodegen") }`
Gradle 9.x の strict implicit-dependency 検出への対処。`runKtlintCheckOverMainSourceSet` が `build/generated-sources/jooq` を入力に持つため、`jooqCodegen` への依存を明示する必要がある。

### `.editorconfig`
ktlint・IntelliJ が共通参照する設定。生成コードを `[**/generated-sources/**]` で除外している。

---

## TODO
- **Axon Command / Event の Span**：Axon Framework 5.2.0 で `axon-tracing-opentelemetry` の autoconfig が復活予定 ( [issue #3594](https://github.com/AxonIQ/AxonFramework/issues/3594) )。 5.2 にバージョン上げて再度試す
- **Event スキーマバージョニング戦略**：Event Sourcing 前提なので将来必須
- Commaand, Eventのインターセプター
- Saga, ProcessManager周りの仕組みを考える
- 認可の仕組みをどうするか
