# Cognito ＋ Google ソーシャルログインのセットアップ

Cognito を IdP として使い、Google ソーシャルログインを Cognito 経由で利用するための設定手順。

## 全体の流れ

1. Cognito ユーザープールを作成（ドメインも設定）
2. Google 側で OAuth クライアントを作成（client ID / secret を取得）
3. Cognito に Google を ID プロバイダーとして追加
4. アプリケーションクライアントを作成・設定

## 1. Cognito ユーザープールを作成

1. AWS コンソールで Cognito ユーザープールを作成する。

## 2. Google 側で OAuth クライアントを作成

1. Google Cloud Console でプロジェクトを作成する。
2. 「API とサービス」→「認証情報」→「認証情報を作成」→「OAuth クライアント ID」を選ぶ。
3. 作成後、**クライアント ID** と **クライアントシークレット**を控える（手順 3 で Cognito に入力する）。
4. 「承認済みのリダイレクト URI」に、Cognito ドメインの idpresponse エンドポイントを登録する。

   ```text
   https://{Cognito ドメイン}.auth.ap-northeast-1.amazoncognito.com/oauth2/idpresponse
   ```

## 3. Cognito に Google を ID プロバイダーとして追加

1. 「ソーシャルプロバイダーと外部プロバイダー」画面を開く。
2. 「アイデンティティプロバイダーを追加」を押す。
3. 「Google」を選ぶ。
4. 手順 2 で取得した**クライアント ID / クライアントシークレット**を入力する。
5. 「属性マッピング」で **`email` と `email_verified`** をマッピングする（email-based linking と email_verified ガードに必須。ADR 0003 / 0004）。

## 4. アプリケーションクライアントを作成・設定

1. 「アプリケーションクライアント」画面で「アプリケーションクライアントを作成」を押す。この時、アプリケーションシークレットがあるクライアントにする。
2. **クライアント名** と **コールバック URL**（`{アプリのオリジン}/api/auth/callback/oidc`）を入力する。
3. **サインアウト URL** を適切に設定する。
4. **有効な ID プロバイダー**に、手順 3 で追加した **Google** を含める。
5. 「OpenID Connect のスコープ」に **`profile`** を含める（フロントの表示名＝`name` claim 取得に使う。ADR 0003）。
