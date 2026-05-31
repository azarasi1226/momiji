# Keycloak Realm Import

`momiji-realm.json` は Keycloak 起動時 ( `start-dev --import-realm` ) に自動 import される realm 定義。
JSON 自体にコメントが書けないため、 このファイルで各セクションを解説する。

---

## realm 全体設定

| キー | 値 | 意味 |
|---|---|---|
| `realm` | `momiji` | realm 名。 frontend の `AUTH_KEYCLOAK_ISSUER` の末尾と一致させる必要あり |
| `enabled` | `true` | realm が利用可能 |
| `sslRequired` | `"none"` | HTTPS 必須を無効化。 **ローカル開発専用、 本番は `"external"` 以上必須** |
| `loginWithEmailAllowed` | `true` | username の代わりに email でログイン可能 |
| `duplicateEmailsAllowed` | `true` | realm 内で重複 email を許可。 backend は issuer+subject で識別するので影響なし |

> ⚠️ **本番運用する場合は以下を必ず追加**:
> - `accessTokenLifespan`: アクセストークン有効期限 ( 秒 )、 デフォ 300 ( 5 分 ) は短い
> - `ssoSessionIdleTimeout`: idle で session 切るまでの秒数
> - `ssoSessionMaxLifespan`: 操作してても強制ログアウトされる秒数

---

## roles

| ロール | 用途 |
|---|---|
| `user` | 全ログインユーザーが付与される。 backend 側で認可判定に使う想定 |

---

## clients

### `momiji-frontend` ( BFF 用 confidential client )

Next.js の BFF ( `auth.ts` の Keycloak provider ) が使う client。

| 設定 | 値 | 意味 |
|---|---|---|
| `publicClient` | `false` | secret を保持できる confidential client。 BFF はサーバーサイドなので secret 保持可 |
| `secret` | `momiji-frontend-secret` | BFF 側の `AUTH_KEYCLOAK_SECRET` 環境変数と一致させる |
| `standardFlowEnabled` | `true` | Authorization Code Flow を有効化 ( BFF が使うフロー ) |
| `directAccessGrantsEnabled` | `false` | Password Grant ( username/password 直送り ) は無効。 BFF からは使わないので OFF |
| `redirectUris` | `localhost:3000/api/auth/callback/keycloak` 等 | NextAuth.js の callback エンドポイント |
| `webOrigins` | `localhost:3000` | CORS 許可元 |
| `defaultClientScopes` | `[openid, email, profile]` | リクエスト時に自動付与される scope |
| `attributes.pkce.code.challenge.method` | `S256` | **PKCE 必須化** ( CSRF / Auth Code Interception 攻撃対策 ) |

### `momiji-api` ( 開発用 public client )

Postman / curl 等から直接アクセストークンを取って backend を叩く時用。

| 設定 | 値 | 意味 |
|---|---|---|
| `publicClient` | `true` | secret 不要 ( 開発便利目的 ) |
| `directAccessGrantsEnabled` | `true` | username/password 直送りでトークン取得可能。 **本番では絶対 OFF** |
| `redirectUris` | `localhost:9090/*` + `oauth.pstmn.io/v1/callback` | Postman の OAuth テスト URL を含む |

### Protocol Mappers ( 両 client 共通 )

| mapper | 機能 |
|---|---|
| `subject` ( `oidc-sub-mapper` ) | OIDC 標準の `sub` claim を access token / id token に含める |
| `identity-provider` ( `oidc-usersessionmodel-note-mapper` ) | session note の `identity_provider` を `identity_provider` claim として token に乗せる。 backend は **どの IDP ( Keycloak ローカル or Google or GitHub ) 経由でログインしたか** をこの claim で判定する |

---

## users

### `testuser`

ローカル動作確認用のテストユーザー。

| 項目 | 値 |
|---|---|
| username | `testuser` |
| password | `password` |
| email | `testuser@example.com` |
| `emailVerified` | `true` ( momiji の `email_verified=true 必須` ロジックを通過するため ) |
| roles | `[user]` |

---

## 設定変更後の反映

`momiji-realm.json` を編集した場合、 既存の Keycloak volume が残ってると **再 import されない** ( volume 内の DB が優先される )。 設定変更を反映したい時は:

```bash
docker compose -f local/docker-compose.yaml down -v
docker compose -f local/docker-compose.yaml up -d
```

で volume ごと吹き飛ばして起動 ( testuser や session が消えるので注意 )。

---

## 本番化チェックリスト ( このファイルを prod に使う場合 )

- [ ] `sslRequired` → `"external"` 以上に
- [ ] `momiji-api` client → 削除、 または `directAccessGrantsEnabled: false`
- [ ] `accessTokenLifespan` 等の token lifespan を明示
- [ ] `users` セクションから `testuser` 削除
- [ ] `secret` を本物の secret に差し替え
- [ ] Identity Provider ( Google / GitHub 等 ) を `identityProviders` セクションで追加
