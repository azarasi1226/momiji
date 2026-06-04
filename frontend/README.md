This is a [Next.js](https://nextjs.org) project bootstrapped with [`create-next-app`](https://nextjs.org/docs/app/api-reference/cli/create-next-app).

## Getting Started

First, run the development server:

```bash
npm run dev
# or
yarn dev
# or
pnpm dev
# or
bun dev
```

Open [http://localhost:3000](http://localhost:3000) with your browser to see the result.

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

`AUTH_PROVIDER` で local(Keycloak) / prod(Cognito) を環境ごとに切り替える ([ADR 0003](../docs/adr/0003-idp-linking.md) の 2 IDP 運用)。
有効化した provider 側の `<PROVIDER>_CLIENT_ID` / `_CLIENT_SECRET` / `<PROVIDER>_ISSUER` だけ設定すればよい。

You can start editing the page by modifying `app/page.tsx`. The page auto-updates as you edit the file.

This project uses [`next/font`](https://nextjs.org/docs/app/building-your-application/optimizing/fonts) to automatically optimize and load [Geist](https://vercel.com/font), a new font family for Vercel.

## Learn More

To learn more about Next.js, take a look at the following resources:

- [Next.js Documentation](https://nextjs.org/docs) - learn about Next.js features and API.
- [Learn Next.js](https://nextjs.org/learn) - an interactive Next.js tutorial.

You can check out [the Next.js GitHub repository](https://github.com/vercel/next.js) - your feedback and contributions are welcome!

## Deploy on Vercel

The easiest way to deploy your Next.js app is to use the [Vercel Platform](https://vercel.com/new?utm_medium=default-template&filter=next.js&utm_source=create-next-app&utm_campaign=create-next-app-readme) from the creators of Next.js.

Check out our [Next.js deployment documentation](https://nextjs.org/docs/app/building-your-application/deploying) for more details.
