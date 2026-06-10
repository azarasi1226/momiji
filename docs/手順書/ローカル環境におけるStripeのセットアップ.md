# ローカル環境における Stripe のセットアップ手順

Stripe をテストモードで使い、ローカル環境からつなぎこむための設定手順。

## 全体の流れ

1. Stripe アカウントを作成し、テストキーを取得
2. Stripe CLI でログイン（初回のみ）
3. webhook 転送を起動し、署名シークレットを入手
4. 3 つのキーをまとめて配置（backend / frontend）
5. 動作確認

## 1. Stripe アカウントとテストキー

1. [Stripe ダッシュボード](https://dashboard.stripe.com/)でアカウントを作成する。  
Google アカウントでログインするのが多分楽、アカウントを作成した直後は TestMode となっており、勝手に課金されることは無いので安心して。

2. 「テストツール」画面で2つのキーを控える。
   - **公開可能キー**（`pk_test_...`）: ブラウザに出る前提のキー
   - **シークレットキー**（`sk_test_...`）: サーバー専用。**コミット・共有は厳禁**

## 2. Stripe CLI（初回のみログイン）

ログインコマンドを叩くと、ブラウザが起動するのでログイン → アクセスを許可する。

```bash
stripe login
```

## 3. webhook 転送の起動（署名シークレットの入手）

Stripe CLI がテストモードのイベントを `localhost:9090/api/webhooks/stripe` に転送する。

```bash
task stripe-listen
```

この時、起動時に表示される **`whsec_...`** を控える（3 つ目のキー）  
このキーが webhook の Secret となる。

## 4. キーの配置

backend / frontend にそれぞれ1つづつシークレットを設定するための環境変数ファイルを作成する。  
そのファイルに、ここまでで揃った 3 つのキーをまとめて配置する。  

> ※どちらのファイルも gitignore 対象（コミットされない）。

| キー | 置き場所 | 変数名 |
|---|---|---|
| シークレットキー（`sk_test_`） | `backend/src/main/resources/local.secret.properties` | `STRIPE_SECRET_KEY` |
| webhook 署名シークレット（`whsec_`） | `backend/src/main/resources/local.secret.properties` | `STRIPE_WEBHOOK_SECRET` |
| 公開可能キー（`pk_test_`） | `frontend/.env.local` | `NEXT_PUBLIC_STRIPE_PUBLISHABLE_KEY` |

## 5. 動作確認

1. `task docker-up` → backend → frontend を起動し、ログインする。
2. 「プロフィール → 支払い方法 → 管理する」からカード追加。テストカードは **`4242 4242 4242 4242`**（有効期限・CVC は任意の未来値）。
3. `stripe listen` のターミナルに `setup_intent.succeeded` と転送結果 `200` が出る → 少し待つと一覧にカードが表示される。
