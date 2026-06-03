# Keycloak Momiji Realm 解説

realm とは client をまとめる認証空間のことであり、 cognito の userPool のようなものである。  
 `momiji-realm.json` は Keycloak 起動時に自動 import される設定ファイルであり、 JSON 自体にコメントが書けないため、 このファイルで各セクションを解説する。

<br>

## realm 全体設定

| キー | 値 | 意味 |
|---|---|---|
| `realm` | `momiji` | realm 名|
| `enabled` | `true` | realm が利用可能 |
| `sslRequired` | `"none"` | HTTPS 必須を無効化 |
| `loginWithEmailAllowed` | `true` | username の代わりに email でログイン可能 |
| `duplicateEmailsAllowed` | `true` | realm 内で email が重複することを許可する。momijiでは同一emailでも違う種類のソーシャルIDPをリンク機能によって紐づける思想なのでその検証ができるようにするために "true" |
| `registrationAllowed` | `true` | ユーザー自身にアカウントを作らせるか？ |
| `resetPasswordAllowed` | `true` | ユーザー自身にパスワードをリセットさせるか？ |
| `rememberMe` | `true` | ログイン画面に「Remember me」 チェックボックスが出る |
| `verifyEmail` | `true` | sign-up 後に持ち主のメールアドレスかを確認する。 momiji では email_verified が true な者しか内部アカウント作成ができない仕組みを取り入れているため "true"|

<br>

## Clientの設定

### 基本設定

| 設定 | 値 | 意味 |
|---|---|---|
| `publicClient` | `false` | secret を保持できる confidential client。 BFF はサーバーサイドなので secret を保持する |
| `secret` | `momiji-frontend-secret` | BFF 側の `KEYCLOAK_CLIENT_SECRET` 環境変数と一致させる |
| `standardFlowEnabled` | `true` | Authorization Code Flow を有効化する。コレは最も一般的で安全なログイン方式である。 |
| `directAccessGrantsEnabled` | `false` | Password Grant ( username/password 直送り ) は無効。 BFF からは使わないので OFF |
| `redirectUris` | `<３つのredirectUrls>` | BFF内の `ログインリダイレクト用`, `ログアウトリダイレクト用`, `Postman検証用`で３つ登録されている |
| `webOrigins` | `localhost:3000` | CORS 許可元 |
| `defaultClientScopes` | `[openid, email, profile]` | リクエスト時に自動付与される scope |
| `attributes.pkce.code.challenge.method` | `S256` | **PKCE 必須化** ( CSRF / Auth Code Interception 攻撃対策 ) |

### Protocol Mappers

| mapper | 機能 |
|---|---|
| `subject` | momiji backendではAccessTokenに `subject` と `issuer` が含まれてなければならない。基本的な IDP(cognito, auth0, etc...)ではデフォルトでAccessTokenに これら２つが入っているのだが、 Keycloak はデフォルトで `issuer` しか入っておらず、バックエンドでエラーが起こるので追加している |

<br>

## 設定変更後の反映

`momiji-realm.json` を編集した場合、 既存の Keycloak volume が残ってると **再 import されない** ( volume 内の DB が優先される )。

### 方法1: Keycloak のボリュームだけ作り直す

`down -v` は **MySQL / Axon Server などローカル全ボリュームを消す**ので使わない。Keycloak だけ作り直す:

```bash
# keycloak コンテナを停止・削除
docker compose -f local/docker-compose.yaml rm -sf keycloak
# keycloak のボリュームだけ削除 ( 名前は `docker volume ls | grep keycloak` で確認 )
docker volume rm momiji_momiji-keycloak-data
# 再作成 → realm.json が再 import される
docker compose -f local/docker-compose.yaml up -d keycloak
```

※この方法でも **realm.json に無いものは消える**: 管理コンソールで手動設定した **Google IdP ( client secret 含む )** や Keycloak 内に作成したユーザー。再設定が必要。