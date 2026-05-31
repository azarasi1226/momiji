# ADR 0003: Identity Provider 連携と独自リンク戦略

- **ステータス**: 採用 (Keycloak / Cognito の 2 IDP で運用中)
- **作成日**: 2026-05-31
- **関連 ADR**: なし (新規)

## コンテキスト

momiji は複数の Identity Provider を採用する ( local 開発: Keycloak、 prod: Cognito )。 ユーザーは Google 等の social IDP を経由してログインし、 IDP 経由で `oidcIssuer` / `oidcSubject` / `email` / `email_verified` 等の claim を受け取る。

ここで「同じ人物が複数の IDP からログインしても、 momiji の内部 user としては 1 つに集約したい」 という要件が出る。 例: 開発者が local 環境で Keycloak 経由で Google ログイン → prod に切り替えて Cognito 経由で同じ Google アカウントでログイン、 のような流れでも user データの一貫性を保ちたい。

### IDP 各サービスの自動 linking 機能

調査の結果、 主要 IDP は **デフォルトでは自動 account linking しない** ( 公式ドキュメント確認済み ):

| サービス | デフォルト | linking を有効化する方法 |
|---|---|---|
| Cognito | しない | `AdminLinkProviderForUser` API、 または pre sign-up Lambda trigger |
| Keycloak | しない | First Login Flow に `Automatically Link Brokered Account` / `Detect Existing Broker User` authenticator を組み込む |
| Auth0 | しない | Rules / Actions / Account Link Extension で実装 |

**共通の理由**: 自動 linking は **account takeover の security risk** がある。 攻撃者が target の email を verified と主張する malicious IDP ( 自前 SAML/OIDC provider 等 ) を構築して sign-in すると、 同 email の既存 user に自動的に link されてアカウントを乗っ取れる。 Auth0 のドキュメントは「Insecurely linking accounts can allow malicious actors to access legitimate user accounts」 と明示的に警告している。

このため 3 サービスとも「デフォルトは別 user として扱い、 linking は admin / 開発者がセキュリティ要件を満たした上で意図的に実装する」 設計になっている。

## 決定

**各 IDP 側の自動 linking 機能は使わず、 momiji 内で独自のリンク戦略を実装する。**

具体的には [CreateUserCommandHandler](../../backend/src/main/kotlin/jp/momiji/feature/user/create/CreateUserCommandHandler.kt) に以下のロジックを集約:

```
① 冪等性チェック: oidcIssuer + oidcSubject が既存登録あれば no-op
② email_verified=false なら fail-closed で拒否
③ 同 email の既存 user がいれば ExternalIdentityLinkedEvent で link
④ どれにも当てはまらなければ UserCreatedEvent + ExternalIdentityLinkedEvent で新規作成 + link
```

### user 識別の基本構造

- **内部 user 識別子**: ULID で生成する momiji 独自の `userId` ( `User` aggregate の identity )
- **外部 IDP 識別子**: `oidcIssuer` + `oidcSubject` の組み合わせ ( OIDC 仕様で「issuer + subject pair はグローバル一意」 が保証される )
- **リンクテーブル**: `LOOKUP_EXTERNAL_IDENTITIES` で `(userId, oidcIssuer, oidcSubject, oidcIdentityProvider)` を保持
- 1 つの `userId` に複数の `(oidcIssuer, oidcSubject)` ペアが link 可能 = 同じ人が Keycloak/Cognito 両方からログインしても 1 user として扱われる

### セキュリティガード (重要)

独自 linking 機構で account takeover 攻撃を防ぐため、 **2 段階の防御**を入れる:

#### ガード 1: whitelist IDP のみ受け入れ

- 受け入れる IDP は [IdentityProvider](../../backend/src/main/kotlin/jp/momiji/domain/idp/IdentityProvider.kt) enum で whitelist 列挙 ( 現在は `LOCAL` / `GOOGLE` )
- [IdentityProviderResolver](../../backend/src/main/kotlin/jp/momiji/domain/idp/IdentityProviderResolver.kt) で whitelist 違反は fail-closed で `UseCaseException` を投げる
- これにより:
  - 攻撃者が malicious SAML/OIDC provider を Cognito / Keycloak 側に追加しても、 momiji の whitelist に無い `providerType` は受け入れ拒否される
  - 「IDP 経由なら何でも信頼する」 という危険な前提を排除し、 **明示的に審査した IDP のみを信頼境界内に入れる**

#### ガード 2: email_verified=true のみ受け入れ

- [CreateUserCommandHandler:32-34](../../backend/src/main/kotlin/jp/momiji/feature/user/create/CreateUserCommandHandler.kt) で `email_verified=false` の sign-in は拒否
- これにより:
  - 攻撃者が「verified ではない任意 email」 を主張しても拒否される
  - whitelist IDP ( Google ) は信頼境界内なので、 そこから「verified」 と返ってきた email の主張は信頼できる
  - つまり「whitelist IDP が verified と主張する email」 = 信頼可能、 として email-based linking ( ③ ) を行う

#### 2 ガードの組み合わせで成立する論証

- 同 email linking は強力 ( UX 上の利便性 ) だが、 単独だと account takeover の risk がある
- **「whitelist IDP × email_verified」 の AND 条件が成立する email のみ**を linking 対象とすることで、 momiji が信頼境界として認める email 主張のみが link 経路に流れる
- whitelist 外の IDP からの email や、 verified=false の email は link 経路に到達できない ( 上流で fail-closed )

### IDP 配列形式 ( Cognito の `identities` ) との関係

Cognito の `AdminGetUser` から取得する `identities` user attribute は JSON 配列形式:

```json
[{"providerType":"Google","providerName":"Google","userId":"...","primary":false,...}]
```

配列構造になっているのは、 `AdminLinkProviderForUser` で複数 IDP を 1 profile に link した場合に複数要素が並ぶため。 公式 AdminGetUser のサンプル例でも、 linking していない user の `identities` は **1 要素のみ** ( `primary: false` フラグの値は linking 状態とは無関係 ) であることを確認済み。

**momiji は Cognito の auto linking ( `AdminLinkProviderForUser` ) を使わない**ため、 `identities` 配列は常に 1 要素。 `firstOrNull()` で先頭 1 要素を取り出して `providerType` で whitelist 判定する実装で問題ない ( [CognitoUserClient](../../backend/src/main/kotlin/jp/momiji/feature/idp/CognitoUserClient.kt) )。

同様に Keycloak も First Login Flow に `Automatically Link Brokered Account` 等の authenticator を入れていないため、 `identity_provider` claim も常に単一の IDP alias を返す ( [KeycloakUserClient](../../backend/src/main/kotlin/jp/momiji/feature/idp/KeycloakUserClient.kt) )。

## 妥協点

### 1. なぜ IDP 側の auto linking 機能を使わないか

- 各 IDP の auto linking 設定 ( Cognito の `AdminLinkProviderForUser`、 Keycloak の `Automatically Link Brokered Account` 等 ) は IDP ベンダ依存で、 セキュリティ要件 ( whitelist + email_verified の AND ) を統一的に表現しにくい
- IDP を切り替える ( 例: 将来 Auth0 に移行 ) たびに linking 戦略を再設計するコストを避けるため、 **アプリケーション層に linking ロジックを集約**して IDP 依存度を下げる
- momiji の linking 判定 ( email-based ) は CQRS の Event Sourcing 経路に乗るため、 監査ログ ( event log ) で linking の根拠を後追い可能

### 2. なぜ email-based linking を採用するか

- 「同じ email = 同じ人」 という暗黙の前提は危険 ( 上述 ) だが、 「**whitelist IDP が verified と保証した email**」 に限れば信頼境界内
- ユーザー体験上、 「同じ Google アカウントなら同じ momiji ユーザー」 が直感的で、 別 IDP 経由で別 user になると混乱する
- 上記 2 ガードで攻撃面を絞った上で email-based linking を採用、 という trade-off

### 3. 将来 IDP を追加する時の作業

新しい IDP ( 例: Microsoft、 Facebook ) を whitelist に追加したい場合:

1. `IdentityProvider` enum に 1 行追加
2. `IdentityProviderResolver` の `when` に分岐追加 + 対応 abstract property を 1 個追加 ( **コンパイルエラーで強制** )
3. 各 IDP Client ( Keycloak / Cognito ) で新 abstract property を override ( **コンパイルエラーで強制** )

「片肺 ( enum 側だけ追加 / 実装側 mapping 漏れ ) 」 状態はコンパイルエラーで検知される。 詳細は `IdentityProvider.kt` の KDoc を参照。

### 4. email_verified が信頼できない場合のリスク

`email_verified` claim を IDP が嘘をついた ( malicious IDP の場合 ) と本来 momiji 側で再検証すべきだが、 これは **ガード 1 ( whitelist ) で排除している前提**で成立する。 つまり「whitelist 入りした IDP は email_verified claim について嘘をつかない」 という信頼を、 IDP 採用時のレビュープロセスで担保する。

## 帰結

良かった点:

- 各 IDP の auto linking 設定差異に依存せず、 一貫した linking 戦略を維持できる
- whitelist + email_verified の 2 ガードが明示的に code 上に現れ、 セキュリティ判断の根拠が追跡可能
- 新 IDP 追加時の作業がコンパイルエラーで強制され、 mapping 漏れの security gap を作れない
- linking の根拠が event log に残るので監査追跡可能

注意すべき点:

- IDP 側の linking 機能を OFF に保つ運用が必要 ( Cognito で `AdminLinkProviderForUser` を呼ばない、 Keycloak First Login Flow を auto link しない設定で保つ )
- email_verified の信頼性は IDP 採用時のレビューに依存。 新 IDP を whitelist に追加する時は「verified email の信頼性」 を必ず審査する
- 将来 linking を IDP 側に移すケース ( 例: Cognito の `AdminLinkProviderForUser` を使う運用に変更 ) では `CognitoUserClient` の「`identities` 配列 1 要素前提」 が崩れるため再設計が必要

## 関連

- [ADR 0001](./0001-value-objects.md): `IdentityProvider` enum と値オブジェクト集約の文脈
- [ADR 0002](./0002-grpc-error-response.md): `UseCaseException` ( whitelist 違反時 ) の gRPC 構造化エラー変換経路
- ソースコード:
  - `backend/src/main/kotlin/jp/momiji/domain/idp/` (IdentityProvider, IdentityProviderResolver)
  - `backend/src/main/kotlin/jp/momiji/feature/idp/` (KeycloakUserClient, CognitoUserClient)
  - `backend/src/main/kotlin/jp/momiji/feature/user/create/CreateUserCommandHandler.kt` (独自 linking ロジック)
- 公式ドキュメント:
  - [AWS Cognito: Linking federated users](https://docs.aws.amazon.com/cognito/latest/developerguide/cognito-user-pools-identity-federation-consolidate-users.html)
  - [Auth0: User account linking](https://auth0.com/docs/manage-users/user-accounts/user-account-linking)
  - Keycloak: Server Administration Guide § First Login Flow
