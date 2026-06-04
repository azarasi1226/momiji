# CLAUDE.md

## プロジェクト概要

ユーザーの認証・アカウント管理サービス。**CQRS + Event Sourcing**（Axon Framework 5.1.1 の DCB）で構築し、外部 IdP（ローカル: Keycloak / 本番: Cognito）と連携する。フロントは BFF（Next.js + Auth.js）。

- **docs**: ドキュメント、ADR等
- **backend**: Kotlin / Spring Boot 4 / Axon 5.1.1（DCB）/ jOOQ + MySQL / gRPC(Connect)
- **frontend**: Next.js + Auth.js v5（BFF パターン、gRPC で backend を叩く）
- **local/**: docker-compose（Axon Server / MySQL / Keycloak / Mailpit / Prometheus / Tempo / Grafana）

## ビルド・テスト・実行

### backend（Java 25 必須）

```bash
# Bash ツール環境には JAVA_HOME が無いので明示する（corretto 25）
JAVA_HOME="/c/Users/seal1/.jdks/corretto-25.0.2" ./gradlew compileKotlin compileTestKotlin
JAVA_HOME="/c/Users/seal1/.jdks/corretto-25.0.2" ./gradlew test            # 全テスト（Testcontainers 使用）
JAVA_HOME="/c/Users/seal1/.jdks/corretto-25.0.2" ./gradlew test --tests "*XxxTest"
./gradlew bootRun   # 起動（profile=local が既定）。要 Keycloak/MySQL 先行起動
```

- ポート: HTTP `9090` / gRPC `9091`
- CommandHandler テストは Axon の `given/when/then` フィクスチャ（in-memory DCB）

### frontend

```bash
pnpm dev        # localhost:3000
pnpm lint
npx tsc --noEmit
```

### ローカル依存（docker）

```bash
docker compose -f local/docker-compose.yaml up -d
```
Keycloak `8085` / MySQL `3336` / Mailpit `8025` / Grafana `3001` / Prometheus `19090`

## アーキテクチャ / パッケージ構成（backend `jp.momiji`）

```
domain/        … 値オブジェクト（Result でバリデーション集約）、エラー型、idp ドメイン
event/         … 永続化イベント（@Event 付与）
feature/       … ユースケース縦切り（create/update/delete/changeemail/findbyid）+ port(interface)
infrastructure/… 外部アダプタ（idp: Cognito/Keycloak、mail: Smtp）★port/adapter 分離
projection/    … read model / lookup テーブル更新（@EventHandler）
config/        … Bean 配線（gRPC, jOOQ, Cognito 等）
```

- **port は feature 側 / adapter は infrastructure 側**（依存を内向きに保つ）。DB(jOOQ) は port 化しない（差し替えない・実DBでテスト）

## 重要な規約・設計判断

- **イベント型名は `@Event(namespace = "momiji.user", name = "<クラスのシンプル名>")` で固定する**。未指定だと型名がパッケージ/クラス名由来になり、リネームで再生が壊れる（→ ADR 0007）。`name` はクラス名と一致させる規約
- **エラー分類**: `BusinessException`(ルール違反) / `ValidationException`(値検証、集約) / unknown(想定外、correlationId 付き)。gRPC `Status.details` に `ErrorDetail` で乗せる（→ ADR 0002）。frontend は `lib/grpc-error.ts` で復元
- **値オブジェクトは `create()` が Result を返す**。fail-fast でなくエラー蓄積（→ ADR 0001）
- **CommandHandler から lookup テーブルを直接読むのは意図的**（SubscriptionEventProcessor で同期更新。CQRS lookup 設計）
- **CommandHandler の State は `created` だけでなく削除も考慮する**。新イベント種別を足したら全 State の `evolve`/ガードを見直す（過去に削除済みユーザーで update が通るバグあり）
- **Event Handler から呼ぶ外部副作用は冪等に**。対象不在は例外でなくログ（warn）で握る
- **2 IdP 運用**（→ ADR 0003）: 1 環境 1 IdP。アプリ層で identity リンク（同一 email → 既存ユーザーに `ExternalIdentityLinkedEvent`）

## frontend（Auth.js / IdP）

- `AUTH_PROVIDER` で keycloak / cognito を切替（`lib/idp.ts`）。`.env.example` 参照
- provider id は **`oidc` 固定** → callback URL は常に `/api/auth/callback/oidc`（IdP 側の redirect URI もこれを登録）
- **ログアウトは 2 段階**: `signOut({ redirect:false })`（アプリ session 破棄）→ `next/navigation` の `redirect()` で IdP の logout へ。Auth.js の `redirectTo` は**外部オリジンへ飛べない**（baseUrl に丸める）ため
- env: `AUTH_SECRET` / `AUTH_URL` は Auth.js v5 規約。provider 設定は `<PROVIDER>_CLIENT_ID` / `_CLIENT_SECRET` / `<PROVIDER>_ISSUER`（id を `oidc` 固定にしているため Auth.js の自動推論は使わず `lib/idp.ts` で明示読み）

## 落とし穴（ハマりどころ）

- **`@Event` の値を変えるとイベントストア互換が壊れる**。本番（Axon Server）は型名移行とセットでデプロイ（dev はボリューム破棄でOK）
- **Keycloak realm を再インポート（`down -v`）すると、admin コンソールで手動設定した Google IdP（client secret 含む）が消える**。realm.json に無いため。ボリュームを消さず反映したいときは Partial Import（REST）を使う
- **`jwtDecoder` は起動時に Keycloak の discovery を eager に取得する**。Keycloak が ready になってから backend を起動する（順序依存）
- **backend の `JWT_ISSUER_URI` と frontend の `AUTH_PROVIDER` を揃える**（Keycloak ↔ Cognito の取り違えで token 検証が落ちる）
- 推測で「○○仕様で〜」とコメントに書かない。外部サービス仕様は公式ドキュメント/実装で確認してから書く

## ADR

設計判断は `docs/adr/` を参照（0001 バリデーション / 0002 エラー分類 / 0003 IDP連携 / 0004 sub マッピング / 0005 テスト方針 / 0006 Cognito / 0007 イベント型名 / 0008 プロジェクション日時は @Timestamp / 0009 EventProcessor をコードに型安全定義・subscribing/PooledStreaming で命名規約を分ける）。
