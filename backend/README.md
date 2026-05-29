# Momiji Backend

基本理念は **CQRS + Event Sourcing + DCB（Dynamic Consistency Boundary）**。
実装には [Axon Framework 5](https://www.axoniq.io/) を採用し、コードは **vertical slice アーキテクチャ**（ユースケース単位のフォルダ分割）で並べている。

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

| 項目 | 採用                                   |
|---|--------------------------------------|
| 言語 | Kotlin                               |
| ランタイム | Java 25                              |
| アプリケーション | Spring Boot 4系                       |
| API | gRPC                                 |
| Event Store / CQRS基盤 | Axon Framework 5.1.1 + Axon Server   |
| Read 側のクエリ | jOOQ                                 |
| DB | MySQL                                |
| DBスキーマ管理 | Atlas（`database/schema.mysql.sql` をマスタとし、jOOQ がそれを読んでコード生成） |
| 認証 | OIDC（Keycloak / Cognito 等）+ JWT      |
| Lint | ktlint（Gradleプラグイン）+ `.editorconfig` |

---

## ディレクトリ構造

```
backend/src/main/kotlin/jp/momiji/
├─ DemoApplication.kt    … Spring Boot エントリポイント
├─ config/               … 設定群（Axon / jOOQ / Security / gRPC）
├─ events/               … ドメインイベント
├─ feature/              … 1ユースケース1パッケージ（vertical slice）
└─ projection/           … Read model 構築（Projector）
```

各ユースケース（`feature/xxx/`）の原則

| ファイル | 役割 |
|---|---|
| `XxxCommand.kt` | Command + Result + Gateway拡張 |
| `XxxCommandHandler.kt` | `@CommandHandler` 本体（必要なら State も同居） |
| `XxxEventHandler.kt` | イベント受けの副作用（Lookup更新・外部連携） |
| `XxxGrpcService.kt` | gRPC受口（実装は薄く保つ） |

※ そのユースケースに特化したクラスが増える場合は適宜追加。あくまで「このユースケースに関するコードはこのパッケージに集約されている」という状態を目指す。

---

## 起動と開発フロー

リポジトリルートに [Taskfile](https://taskfile.dev/) を用意してある。ローカル環境の上げ下げ・コード生成・DBマイグレーションは `task` 経由で叩く前提。`task --list-all` で全タスク一覧。

### 必要なもの

- JDK 25
- Docker / Docker Compose
- [`aqua`](https://aquaproj.github.io/) — CLIバージョン管理ツール。リポジトリルートの [`aqua.yaml`](../aqua.yaml) に `task` / `buf` / `atlas` / `pnpm` のバージョンが宣言されていて、`aqua install` で全部入る。

```bash
aqua install   # task / buf / atlas / pnpm をインストール
```

### 初回セットアップ（リポジトリルートで実行）

```bash
task local-up              # MySQL / Axon Server / Keycloak をコンテナで起動
task buf-generate          # proto → Kotlin / TypeScript 生成
task atlas-migrate-apply   # マイグレーション適用（local-up に依存しているので未起動でも自動で立ち上がる）

cd backend
./gradlew build
```

### バックエンドの起動

```bash
cd backend
./gradlew bootRun
```

---

## プロファイルと環境変数

`spring.profiles.active` で環境を切り替える。デフォルトは `local`。

| プロファイル | 構成 | 用途 |
|---|---|---|
| `local`（デフォルト） | app-common + app-local + datastore-mysql + idp-keycloak + mail | ローカル開発（Docker Compose 経由） |
| `test` | app-common + app-test + datastore-mysql + idp-cognito + mail | CI/テスト環境（実Cognito） |
| `prod` | app-common + app-prod + datastore-mysql + idp-cognito + mail | 本番 |
| `integration-test` | app-integration-test のみ | 統合テスト（DataSource等はTestContainers側で組む想定） |

### テスト・本番環境にて必須の環境変数（fail-fast）

未設定だと **起動時に例外で落ちる**。`local` profile では [application-app-local.yaml](src/main/resources/application-app-local.yaml) が値を提供するので環境変数の設定は不要。

| 環境変数 | 必須profile | 説明 |
|---|---|---|
| `JWT_ISSUER_URI` | test / prod | OIDC issuer URI（Cognitoなら `https://cognito-idp.{region}.amazonaws.com/{poolId}`） |
| `EMAIL_CHANGE_SECRET` | test / prod | メール変更トークン署名鍵（**HS256のため32byte以上必須**、[EmailChangeTokenService](src/main/kotlin/jp/momiji/feature/user/changeemail/EmailChangeTokenService.kt) で長さ検証あり） |
| `COGNITO_USER_POOL_ID` | test / prod | Cognito User Pool ID |
| `SPRING_DATASOURCE_HOST` | test / prod | MySQL host |
| `SPRING_DATASOURCE_PORT` | test / prod | MySQL port |
| `SPRING_DATASOURCE_DATABASE` | test / prod | MySQL database name |
| `SPRING_DATASOURCE_USERNAME` | test / prod | MySQL username |
| `SPRING_DATASOURCE_PASSWORD` | test / prod | MySQL password |

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

[GrpcConfig](src/main/kotlin/jp/momiji/config/grpc/GrpcConfig.kt) の `grpcExceptionHandler` で `UseCaseException` → `Status.INVALID_ARGUMENT` に変換している。

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
- [ ] **テストコードサンプル拡充**
- [ ] **gRPC Status の細分化**：すべて `INVALID_ARGUMENT` に潰しているのを意味別 (`NOT_FOUND` / `ALREADY_EXISTS` / `PERMISSION_DENIED`) に
- [ ] **観測性**：Micrometer / OpenTelemetry の導入
- [ ] プロファイルによって、環境を切り替える仕組みの導入
- [ ] **Event スキーマバージョニング戦略**：Event Sourcing 前提なので将来必須
- [ ] **AWS Cognito 実装の決着**：[CognitoUserClient](src/main/kotlin/jp/momiji/feature/idp/CognitoUserClient.kt) はコメントアウト中、関連依存も含めて削除 or 復活させるか判断
