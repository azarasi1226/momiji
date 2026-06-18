# Momiji Frontend

Next.js + Auth.js による BFF（Backend For Frontend）。認証・セッション管理を担い、backend gRPC サービスを呼び出してUIを提供する。

## 技術スタック

| 項目 | 採用 |
|---|---|
| フレームワーク | Next.js 15（App Router） |
| 認証 | Auth.js v5（OIDC / Keycloak・Cognito 切替） |
| API通信 | gRPC（Connect） |
| UI | shadcn/ui + Tailwind CSS v4 |
| Linter / Formatter | Biome 2.5 |
| パッケージマネージャ | pnpm |

## 起動と開発

```bash
pnpm dev        # localhost:3000
pnpm lint       # Biome lint
pnpm format     # Biome format（上書き）
pnpm check      # lint + format 自動修正
pnpm typecheck  # TypeScript 型チェック
```

ローカル起動前に docker compose で依存サービスを立ち上げること（[local/](../local/) 参照）。

## 環境変数

ローカル開発は **`.env.development`（commit 済み）** が `next dev` に自動ロードされるため、コピーや事前設定なしでそのまま起動できる（backend の `local.env.properties` と同じ思想）。

秘密値の上書き（本番 Cognito を local で試す等）は **`.env.local`（gitignore）** に書く。`.env.local` は `.env.development` を上書きし、git には載らない（Cognito の実シークレットを誤って commit しないため）。

| 変数 | 必須 | 説明 |
|---|---|---|
| `AUTH_SECRET` | ✅ | Auth.js のセッション暗号化キー。`npx auth secret` で生成 |
| `AUTH_URL` | ローカル ❌ / 本番 ✅ | ログアウト後の戻り先 (post_logout_redirect_uri / logout_uri) の基準 URL。ローカルはリクエストから推論されるため未設定可 (default `http://localhost:3000`)。本番では IdP の「許可されたサインアウト URL」と完全一致させて明示 |
| `AUTH_PROVIDER` | ✅ | 使用する IdP。`keycloak`(ローカル) か `cognito`(本番)。未設定/不正は起動時にエラーで落ちる ([lib/idp.ts](lib/idp.ts)) |
| `KEYCLOAK_CLIENT_ID` / `KEYCLOAK_CLIENT_SECRET` / `KEYCLOAK_ISSUER` | `AUTH_PROVIDER=keycloak` 時 ✅ | Keycloak client の id / secret と realm の OIDC issuer URL |
| `COGNITO_CLIENT_ID` / `COGNITO_CLIENT_SECRET` / `COGNITO_ISSUER` | `AUTH_PROVIDER=cognito` 時 ✅ | Cognito app client の id / secret と issuer (`https://cognito-idp.<region>.amazonaws.com/<userPoolId>`) |
| `GRPC_URL` | ✅ | backend gRPC エンドポイント |
| `IMAGE_PROTOCOL` | ✅ | 商品画像配信の protocol（`http` / `https`）。Next.js の `next.config` で許可ホストに使用 |
| `IMAGE_HOSTNAME` | ✅ | 商品画像配信のホスト名。Next.js の `next.config` で許可ホストに使用 |
| `IMAGE_PORT` | ✅ | 商品画像配信のポート番号。Next.js の `next.config` で許可ホストに使用 |
| `NEXT_PUBLIC_STRIPE_PUBLISHABLE_KEY` | ✅ | Stripe の公開可能キー。クライアントサイドの Stripe.js 初期化に使用 |

`AUTH_PROVIDER` で local(Keycloak) / prod(Cognito) を環境ごとに切り替える（[ADR 0003](../docs/adr/0003-idp-linking.md) の 2 IDP 運用）。
有効化した provider 側の `<PROVIDER>_CLIENT_ID` / `_CLIENT_SECRET` / `<PROVIDER>_ISSUER` だけ設定すればよい。

## TODO

- **`@import "shadcn/tailwind.css"` を一行 import に戻す**
  - radix-nova スタイルが想定する `@import "shadcn/tailwind.css"`（`shadcn` パッケージの `dist/tailwind.css`）を [app/globals.css](app/globals.css) でそのまま import すると、その中の `@theme inline { @keyframes ... }` を turbopack/lightningcss が処理できず、**import 以降の `@theme inline` / `:root` が丸ごと脱落 → テーマカラー(primary 等)が一切効かなくなる**。
  - 暫定対応として、その import をやめ、コンポーネント(select/checkbox 等)が使う custom variant (`data-open` / `data-closed` / `data-checked` …) と `no-scrollbar` を globals.css に**手でインライン展開**している。
  - 原因が turbopack の export `style` 条件の解決側か、lightningcss/Tailwind のパース側かは未確定。`next` / `tailwindcss` / `shadcn` のいずれかの bump で解消する可能性あり。
  - 確認手順: globals.css を一行 import に戻す → `rm -rf .next && pnpm build` → 出力 CSS を `--primary` で grep（出れば解消）。
  - 解消したら、インライン展開した custom variant / utility を消して `@import "shadcn/tailwind.css"` 一行に戻す。新しい radix-nova コンポーネントを `shadcn add` した際にインライン定義の追従漏れが起きるリスクも消える。
