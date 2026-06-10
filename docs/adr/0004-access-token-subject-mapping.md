# ADR 0004: Access Token に `sub` claim　が入っていないとシステムが崩壊する！　ユーザーIDを特定する方法！

- **ステータス**: 採用 ( Keycloak realm.json の両 client に `oidc-sub-mapper` を設定済 )
- **作成日**: 2026-05-31
- **関連 ADR**: [0003 IDP 連携と独自リンク戦略](./0003-idp-linking.md) ( ここで sub 検証経路の前提を確立 )

## コンテキスト

momiji の backend は受け取った access token から `sub` を取り出して、 momiji 内部の `user_id` にマッピングする ( [UserIdResolver](../../backend/src/main/kotlin/jp/momiji/feature/user/UserIdResolver.kt) で `JwtAuthenticationToken.token.subject` を読む経路 ) 。

つまり backend の認可ロジックは **access token に `sub` claim が含まれていることを前提**にしている。

### 経緯 ( なぜ ADR にするか )

設計整理の過程で「OIDC 仕様で `sub` は標準なので access token にも default で乗る」 という誤った認識で realm.json から `oidc-sub-mapper` を削除した。 結果:

- backend で `authentication.token.subject` が `null` になる
- `UserIdResolver` が lookup を null で行い、 fail-closed で「ユーザーが登録されていません」 ( `BusinessError` ) を返す
- 例外ではなく**正常レスポンス + null** で扱われたため、 frontend / backend のログにエラー痕跡が残らず、 デバッグに時間がかかった

この罠を将来繰り返さないよう、 **「subject mapper はなぜ必要か」 を仕様レベルで明文化**しておく。

## 決定

両 client ( `momiji-frontend` / `momiji-api` ) の `protocolMappers` に `oidc-sub-mapper` を明示的に設定し、 access token / id token / introspection token のすべてに `sub` claim を必ず乗せる。

```json
{
  "name": "subject",
  "protocol": "openid-connect",
  "protocolMapper": "oidc-sub-mapper",
  "consentRequired": false,
  "config": {
    "id.token.claim": "true",
    "access.token.claim": "true",
    "introspection.token.claim": "true"
  }
}
```

## なぜ必要か ( この ADR の核心 )

### `sub` claim の仕様レベルの違い

| 仕様 | `sub` claim |
|---|---|
| **OIDC ID Token** ( OIDC Core 1.0 § 2 ) | **REQUIRED** ( `sub` は ID Token の必須 claim ) |
| **OAuth 2.0 Access Token** ( RFC 6749 / RFC 9068 ) | 未規定 ( access token は opaque でも JWT でもよい、 `sub` の有無は IdP 実装依存 ) |
| **OIDC UserInfo Endpoint** ( OIDC Core 1.0 § 5.3 ) | REQUIRED ( userinfo レスポンスには必ず `sub` が含まれる ) |

つまり「OIDC で `sub` は標準だから access token にも default で乗る」 は **仕様の混同による誤解**。 OIDC が必須にしているのは ID Token と userinfo レスポンスの `sub` であって、 access token については OAuth 2.0 のスコープで仕様未規定。

### Keycloak の access token の default 挙動

Keycloak はデフォルトの access token に `sub` を **乗せない設定**で出してくる。 access token に `sub` を含めるには明示的に `oidc-sub-mapper` を client に割り当てる必要がある。

### なぜ ID Token ではなく access token を使うか

代替として「backend に ID Token を送って sub を読む」 選択肢もあるが、 採用しない:

- ID Token は **authentication 結果の証明**で、 API リクエストごとに渡すのは OIDC の意図から外れる
- backend に渡すのは access token、 という OAuth 2.0 / OIDC の標準パターンを踏襲したい
- backend が userinfo endpoint を access token で叩く運用 ( [OidcUserInfoFetcher](../../backend/src/main/kotlin/jp/momiji/feature/user/create/OidcUserInfoFetcher.kt) ) とも整合 ( access token は「リソースアクセスの証明」 として一貫した位置付け )

→ access token を渡しつつ、 そこに `sub` を **明示的に乗せる** ( 本 ADR の決定 ) のが最も標準的。

### なぜ両 client に乗せているか

| 経路 | token 発行元 client | mapper が必要な理由 |
|---|---|---|
| **BFF** ( frontend → backend ) | `momiji-frontend` | frontend が転送する access token に `sub` が要る |
| **Postman 等** ( 開発便利 ) | `momiji-api` | Postman で直接 backend を叩く時の token に `sub` が要る |

backend の検証ロジック ( `JwtAuthenticationToken.token.subject` ) は token の発行元 client を区別しない。 どの client が発行した token であっても `sub` を読みに行くため、 両 client に subject mapper が必要。

## 妥協点 / 検討した代替案

### 代替 A: backend が userinfo endpoint で `sub` を取得 ( 不採用 )

技術的には可能 ( userinfo は OIDC で `sub` 必須 ) 。 ただしリクエストごとに Keycloak への userinfo round-trip が必要で、 latency + Keycloak への負荷が増える。 `sub` は JWT に乗せる方が軽量で、 ネットワーク的にもオフライン検証可能。

### 代替 B: backend が ID Token を受け取る ( 不採用 )

OIDC の使い分けから外れる ( ID Token は authentication 結果、 API authorization は access token を使うのが標準 ) 。 momiji が採用してる BFF パターンとも整合しない。

### 代替 C: 開発便利の `momiji-api` client は削除 ( 将来検討 )

[ADR 0003](./0003-idp-linking.md) の本番化チェックリストにも「`momiji-api` client は削除 or `directAccessGrantsEnabled: false`」 と記載済。 Postman 経由で backend を叩く運用が無いなら、 `momiji-api` client 自体を削除可能で、 その場合 `momiji-api` 側の subject mapper も不要になる。 ただし開発便利の用途を維持するなら現状の両 client 構成のまま。

## 帰結

良かった点:

- access token のみで backend の `sub` 検証が完結する ( 軽量、 userinfo round-trip 不要、 オフライン JWT 検証可能 )
- BFF / Postman 両経路で同じ backend 検証ロジック ( `JwtAuthenticationToken.token.subject` ) が動く
- OAuth 2.0 / OIDC の標準パターンに沿った設計
- access token に **必要最低限の `sub` のみ**を乗せる方針 ( custom claim は乗せない、 IdP 判定は admin REST 経路、 ADR 0003 ) と整合

注意すべき点:

- 「OIDC 仕様で `sub` は標準だから access token にも default で乗る」 という**誤解をしない** ( 仕様レベルでは ID Token と userinfo のみが必須 )
- subject mapper を削除した場合、 backend は `subject = null` を読むが、 **例外ではなく正常レスポンス + null** で扱われるため、 サイレントに「user not found」 になりデバッグが難航する ( 今回の罠 )
- realm.json の mapper 設定変更は **必ず動作確認**してからリリースすること
- 将来 IdP を切り替える場合 ( Cognito 等 ) 、 同様に「access token に `sub` を乗せる」 設定が必要。 Cognito の場合は User Pool の token 設定で確認

## 関連

- [ADR 0003](./0003-idp-linking.md): IdP 連携と独自リンク戦略 ( sub 検証経路の前提 )
- 仕様:
  - [OAuth 2.0 (RFC 6749)](https://datatracker.ietf.org/doc/html/rfc6749) ‐ access token は opaque、 構造は未規定
  - [JWT Profile for OAuth 2.0 Access Tokens (RFC 9068)](https://datatracker.ietf.org/doc/html/rfc9068) ‐ access token を JWT として運ぶ場合の標準。 `sub` は SHOULD ( 強い推奨 ) だが MUST ではない
  - [OIDC Core 1.0 § 2](https://openid.net/specs/openid-connect-core-1_0.html#IDToken) ‐ ID Token の REQUIRED claim に `sub` を含む
  - [OIDC Core 1.0 § 5.3](https://openid.net/specs/openid-connect-core-1_0.html#UserInfoResponse) ‐ userinfo レスポンスに `sub` REQUIRED
- ソースコード:
  - `backend/src/main/kotlin/jp/momiji/feature/user/UserIdResolver.kt` ( `subject` を読む )
  - `local/keycloak/momiji-realm.json` ( subject mapper の定義 )
- 実装ドキュメント: [Keycloak Server Administration Guide § Protocol Mappers](https://www.keycloak.org/docs/latest/server_admin/index.html#_protocol-mappers)
