# 🍁Momiji

**CQRS / Event Sourcing / DCB / 垂直スライスアーキテクチャ**で構築する、フルスタック EC サイトのサンプルプロジェクト

## 🧱 技術スタック

| 領域 | 技術 |
| --- | --- |
| backend | Kotlin / Spring Boot 4 / **Axon Framework 5（DCB）** / jOOQ / gRPC |
| frontend | Next.js（BFF パターン）/ shadcn/ui / tailwind |
| database | PostgreSQL |
| 認証 | OIDC 準拠 IdP（ローカル: Keycloak、本番: Cognito）。破壊的変更なしに交換可能 |
| 決済 | Stripe |
| ローカル環境 | docker （PostgreSQL / Axon Server / Keycloak / Mailpit / MinIO / o11y スタック） |
| 可観測性 | OpenTelemetry / Prometheus / Grafana / Tempo / Loki |

## 📁プロジェクト構造

```:text
momiji
├─ frontend      : (NextJS)
├─ backend       : (server side kotlin)
│   └─ database  : (atlas)
├─ grpc          : (proto + buf 設定)
├─ local         : (docker-compose / 各種ローカル環境構築用設定)
├─ docs
│   ├─ adr/      
│   ├─ 手順書/
│   └─ 設計/
├─ README.md
└─ taskfile.yaml : (プロジェクトで利用するtask一式) 
```

## 🏗️ ローカル環境の構築

### 0. 前提

- [aqua](https://aquaproj.github.io/)（CLI ツールのバージョン管理ツール）
- Docker
- JDK 25（backend の実行用。`JAVA_HOME` を通しておく）
- Node.js 24 ~（frontend の実行用。pnpm 自体は aqua で入るが、Next.js の実行には Node 本体が必要）

### 1. CLI ツールのインストール

aqua をインストール後、以下のコマンドを `aqua.yaml` がある階層(ルート)で実行。

```bash
aqua i
```

### 2. 初回セットアップ

```bash
task setup   # コンテナ起動 → DB マイグレーション → proto コード生成
```

### 3. Stripe の準備（クレジットカードが絡む機能を使わない場合はスキップ）

[docs/手順書/ローカル環境における Stripe のセットアップ手順.md](docs/手順書/ローカル環境におけるStripeのセットアップ.md) を参照

> 簡単に説明すると、 Stripe の無料アカウントを作成し3つのシークレットを取得後、環境変数ファイルを2個作るのだが、あらかじめダミー値がセットされているので、**スキップ**することもできる。その場合、Stripe(クレカ決済)に関する処理はすべてエラーになる。

### 4. backend 起動

```bash
cd backend
./gradlew bootRun   # HTTP: 9090 / gRPC: 9091
```

### 5. seed 実行(初期のサンプルデータ投入)

※ 先にバックエンドが起動されていないと投入できません

```bash
./gradle seedData
```

### 6. frontend 起動

```bash
cd ../frontend
pnpm install   # 初回のみ
pnpm dev       # http://localhost:4000
```

## 🖥️ ローカル環境確認リンク一覧

| サービス | ポート | URL | 備考 |
| --- | --- | --- | --- |
| frontend (NextJS) | 4000 | <http://localhost:4000> | |
| backend gRPC | 9091 | | |
| backend HTTP (Stripe webhook) | 9090 | | |
| Axon Server UI | 8024 | <http://localhost:8024> | |
| PostgreSQL | 5436 | postgres://localhost:5436/momiji | postgres / passw0rd |
| Keycloak | 8085 | <http://localhost:8085> | 管理コンソール: admin / admin |
| Mailpit (SMTP / UI) | 1025 / 8025 | <http://localhost:8025> | |
| MinIO (S3 API / 管理コンソール) | 9000 / 9001 | <http://localhost:9001> | minioadmin / minioadmin |
| Prometheus | 19090 | <http://localhost:19090> | |
| Grafana | 3001 | <http://localhost:3001> | |
| Tempo | 3200 | | UI なし |
| Loki | 3100 | | UI なし |
