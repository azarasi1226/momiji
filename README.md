# 🍁Momiji
CQRS/ES + 垂直スライスアーキテクチャを基盤に作成、 AIとの親和性が高い(はず)
OIDCでマルチアカウントリンクを実現するサンプルプロジェクト

## プロジェクト構造
```
momiji
├─ frontend (NextJS)
├─ backend (server side kotlin)
│   └─ database
├─ README.md
├─ docker-compose.yaml
└─ docs
└─ sample.md
```

## 特徴
* IDP は Cognito, Auth0, Keycloak などのメジャーな OIDC 準拠 IDP を使用でき、破壊的な変更なしにいつでも交換できる
* ログイン方式は複数対応する。IDP 独自のパスワード認証や各種ソーシャルログイン (Google, GitHub など) が利用可能
* 同じユーザーが異なるログイン方法を使っても、email アドレスが同一であれば Momiji 内部では一つのアカウントに紐づく
  > 例: ユーザー A (tanaka@gmail.com) が Google ソーシャルログインで登録済みの状態で、同じ email の GitHub アカウントで初回ログインした場合、両方のソーシャルアカウントが Momiji 内部の同一アカウントに紐づく
* BFF (Next.js) が IDP との認証を担当し、バックエンド (Relying Party) へは Access Token を Bearer ヘッダーで渡して検証する構成とする
* BFF 内部では NextAuth.js を使用し、データベースに ID Token / Access Token / Refresh Token を保存する。Cookie にはセッション ID のみを保管する
* ユーザーは Momiji 内の email アドレスをいつでも変更できる。email アドレスを変更しても、既存のソーシャルログイン紐づけは解除されない。
* BFF 側でログアウトした場合、サーバー上のセッションが破棄される。ブラウザには Token が露出していないため、即時のログアウトが実現できる。

## 懸念

### email 自動リンクによるアカウント乗っ取り

| |                                                                                                                      |
|---|----------------------------------------------------------------------------------------------------------------------|
| **リスク** | 攻撃者が他人の email でソーシャルアカウントを作成し、既存の Momiji アカウントに紐づけてしまう                                                               |
| **対策** | Momiji内アカウント作成時はIDP側の`email_verified` が `true` の場合のみ紐づけを行う。<br/>また、メールアドレスの所有が保証される IDP のみを使用する。 (Google, GitHub など) |

### email 変更後に別人が紐づく可能性

| |                                                                             |
|---|-----------------------------------------------------------------------------|
| **リスク** | Momiji内部のメールアドレス変更機能にて、変更先の email を持つソーシャルアカウントで第三者がログインすると、意図せず同一アカウントに紐づく |
| **対策** | メール変更時にメール検証を行い、所有者確認が完了するまでメールは反映しない                                       |

### IDP 移行時のユーザーデータ引き継ぎ

| |                                                                                               |
|---|-----------------------------------------------------------------------------------------------|
| **リスク** | IDP を切り替えた場合、既存ユーザーとの紐づけが切れる                                                                  |
| **対策** | email を内部のユニークな識別子として使用すれば再紐づけは可能。ただしユーザーに再ログインが必要。<br/>パスワードは IDP 側でハッシュ化されているはずなので再登録になるだろう |

## ログインフロー

```mermaid
sequenceDiagram
    participant B as Browser
    participant BFF as BFF (Next.js)
    participant IDP as IDP
    participant M as Momiji

    B->>BFF: 1. ログイン開始
    BFF->>IDP: 2. /authorize (リダイレクト)
    IDP-->>B: 3. ログイン画面を表示
    B->>IDP: 4. 認証完了
    IDP->>BFF: 5. Authorization Code
    BFF->>IDP: 6. Code → Token 交換
    IDP-->>BFF: Access Token

    Note over BFF, M: ユーザー同期 (毎回ログイン時に実行)
    BFF->>M: 7. POST /users/me (Bearer Token)
    M->>IDP: 7a. GET /userinfo (Bearer Token)
    IDP-->>M: sub, issuer, email, email_verified
    Note over M: issuer + subject で検索<br/>存在 → 既存ユーザーを返す<br/>未登録 & email_verified=true & 同一 email 存在 → 紐づけ<br/>未登録 & email_verified=true & 新規 → 作成<br/>email_verified=false → 拒否
    alt 成功
        M-->>BFF: 200 OK (ユーザー情報)
        BFF-->>B: 8. ログイン完了 (セッション確立)
    else 失敗
        M-->>BFF: Error
        Note over BFF: セッション破棄 & IDP ログアウト
        BFF-->>B: ログイン失敗
    end
```

## ユーザー情報参照フロー

```mermaid
sequenceDiagram
    participant B as Browser
    participant BFF as BFF (Next.js)
    participant M as Momiji

    B->>BFF: 1. データ取得リクエスト
    BFF->>M: 2. GET /users/me (Bearer Token)
    Note over M: Access Token から sub と issuer を取得し<br/>external_identities から内部ユーザー ID を特定
    M-->>BFF: ユーザー情報
    BFF-->>B: レスポンス
```

## ログアウトフロー

```mermaid
sequenceDiagram
    participant B as Browser
    participant BFF as BFF (Next.js)
    participant IDP as IDP

    B->>BFF: 1. ログアウトリクエスト
    Note over BFF: サーバー上のセッションを破棄<br/>(DB から Token を削除)
    BFF->>IDP: 2. RP-Initiated Logout (セッション無効化)
    IDP-->>BFF: OK
    BFF-->>B: 3. ログアウト完了 (Cookie 削除)
    Note over B: ブラウザには Token が存在しないため<br/>即時にログアウトが完了する
```

## 複数のログイン方法で一つのMomiji内部アカウントと紐づける仕組み
| テーブル | 説明 |
|---|---|
| **external_identities** | IDP 内部のユーザーと Momiji 内部のユーザーを紐づけるためのテーブル |
| **users** | Momiji 内部のアカウント情報を管理するテーブル |

* external_identities の主キーは `issuer` + `subject` の複合キーである。subject は同一 IDP 内でユニークだが、IDP が異なれば同じ subject が存在しうる。issuer(IDPを識別する値) を組み合わせることで、IDP を移行しても既存のレコードと衝突しない設計になっている。

```mermaid
erDiagram
    users {
        string id PK "U-001, U-002"
        string email "alice@ex, bob@ex"
    }
    lookup_external_identities {
        string oidc_issuer PK "auth0.com"
        string oidc_subject PK "google|11, github|22"
        string user_id FK "U-001, U-002"
    }
    users ||--o{ lookup_external_identities : ""
```
