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
| `registrationAllowed` | `true` | ログイン画面に「Register」 リンクが出て、 ユーザー自身が local アカウントを sign-up できる。 momiji の LOCAL IDP 経路をローカルで試したい時用 |
| `resetPasswordAllowed` | `true` | ログイン画面に「Forgot Password?」 リンクが出る。 SMTP 設定が無い local では実際にメール送信はされないが UI 確認できる |
| `rememberMe` | `true` | ログイン画面に「Remember me」 チェックボックスが出る |
| `verifyEmail` | `false` | sign-up 後の email 確認メール経路を使わない ( SMTP 設定不要 )。 backend 側で local profile 限定の bypass ( [EmailVerificationPolicy](../../backend/src/main/kotlin/jp/momiji/feature/user/create/EmailVerificationPolicy.kt) ) が効くので、 sign-up が一気通貫で完了する。 **本番では `true` 必須 + SMTP 必須** |

> ⚠️ **本番運用する場合は以下を必ず追加**:
> - `accessTokenLifespan`: アクセストークン有効期限 ( 秒 )、 デフォ 300 ( 5 分 ) は短い
> - `ssoSessionIdleTimeout`: idle で session 切るまでの秒数
> - `ssoSessionMaxLifespan`: 操作してても強制ログアウトされる秒数

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

### Protocol Mappers

| mapper | 機能 |
|---|---|
| `subject` ( `oidc-sub-mapper`、 両 client ) | **OAuth 2.0 access token に `sub` claim を乗せる**ため必須。 OIDC 仕様で `sub` 必須なのは ID Token のみで、 access token は default で含まれないので Keycloak で明示マップする必要あり。 backend ( Spring Security の `JwtAuthenticationToken.token.subject` ) が user 識別子として読む |

`identity_provider` 相当の **custom claim は乗せない**: access token は auth proof として最低限の `sub` のみで「ピュアな OIDC 標準 claim 経路」 にする。 IDP 判定は backend が **Keycloak Admin REST API** ( `/users/{id}/federated-identity` ) で直接問い合わせる ( Cognito の `AdminGetUser` + `identities` attribute と対称の設計、 [ADR 0003](../../docs/adr/0003-idp-linking.md) ) 。

backend が必要とする属性の取り方:

- `sub` → access token の claim から直接 ( `subject` mapper で乗ってる )
- `email` / `email_verified` → backend が access token を持って **userinfo endpoint** に問い合わせ ( [OidcUserInfoFetcher](../../backend/src/main/kotlin/jp/momiji/feature/user/create/OidcUserInfoFetcher.kt) )
- IDP 判定 → backend が **Keycloak Admin REST API** で `/users/{id}/federated-identity` を問い合わせ ( [KeycloakUserClient.fetchFederatedIdentities](../../backend/src/main/kotlin/jp/momiji/feature/idp/KeycloakUserClient.kt) )

---

## users

seed user は無し。 ローカル動作確認は **下記の sign-up 動作確認フローで自分で登録**して進める ( `registrationAllowed: true` で UI から sign up 可能、 mailpit でメール verify する )。

---

## ローカルでの sign-up 動作確認フロー

`verifyEmail: true` + mailpit 連携の組み合わせで、 新規 sign-up は以下の流れで完走できる:

1. http://localhost:3000 の Keycloak login → 「Register」 リンク
2. username / email / first / last / password を入力 → submit
3. Keycloak が user を作成 ( `emailVerified=false` ) + 確認メールを mailpit へ送信
4. http://localhost:8025 ( mailpit UI ) で受信メールを開き、 確認リンクをクリック ( **register したのと同じブラウザ window 内**で開くこと。 別 window だと session 不整合で固まる )
5. Keycloak で `emailVerified=true` に更新 → frontend に redirect
6. backend `CreateUser` が `email_verified=true` のガード 2 を通過 → momiji の user 作成成功

mailpit はメール送信を「受信して表示するだけ」 で実際の外部送信はしない。 SMTP 認証なし ( `auth: false` ) で動く。

**[ADR 0003](../../docs/adr/0003-idp-linking.md) のガード 2 ( email_verified=true 必須 ) はローカル / 本番で同じ厳格さで効く** ( ローカルだけ緩めるような bypass は入れていない ) 。**

本番に近い「mailpit 経由で verify メール → リンククリック」 のフローを試したい場合は、 別途 `verifyEmail: true` + `smtpServer` セクションを realm.json に入れて、 backend の `momiji.email-verification.required` も true に戻す。

---

## 設定変更後の反映

`momiji-realm.json` を編集した場合、 既存の Keycloak volume が残ってると **再 import されない** ( volume 内の DB が優先される )。 設定変更を反映したい時は:

```bash
docker compose -f local/docker-compose.yaml down -v
docker compose -f local/docker-compose.yaml up -d
```

で volume ごと吹き飛ばして起動 ( sign-up した user や active session が消えるので注意、 sign-up からやり直し )。

---

## 本番化チェックリスト ( このファイルを prod に使う場合 )

- [ ] `sslRequired` → `"external"` 以上に
- [ ] `momiji-api` client → 削除、 または `directAccessGrantsEnabled: false`
- [ ] `accessTokenLifespan` 等の token lifespan を明示
- [ ] `secret` を本物の secret に差し替え
- [ ] Identity Provider ( Google / GitHub 等 ) を `identityProviders` セクションで追加
- [ ] `registrationAllowed` の運用判断 ( 自由登録を許すか、 招待制にするか )
- [ ] `smtpServer` → 本番の SMTP ( SES / SendGrid 等 ) に差し替え。 `auth: true`, `starttls: true` 必須
- [ ] `duplicateEmailsAllowed` → `false` ( 本番で email 重複を許すと account takeover 経路になる )
