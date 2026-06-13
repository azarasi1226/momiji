## 認可・ID・冪等性・セキュリティ（要設計／ADR 化）

* 認可（authz）の方式を決める
  * ロールベース（RBAC）。 sub → アカウント → ロール/権限 を引く形
  * 原則: **認証主体は常に実アカウントの sub。 サービスアカウントの特権バイパスを authz に作らない**
  * ADR 化する
* Seed でもアカウント ID（userId/sub）を引けるようにする
  * 今は client_credentials（azp=momiji のサービスアカウント）で momiji ユーザーが無く、 UserIdResolver で解決できない
  * seed を「実アカウントのトークン（Keycloak の password grant）」で叩く方式へ寄せる
    * admin データ（brand/product）: admin ロール付きの実アカウント
    * user 所有データ（配送先/カゴ/カード）: 各テストユーザー
  * 狙い: **認可含め全動作で seed 専用の別経路を作らなくて済む**（authz レイヤーに抜け道を作らない）
  * コスト: realm.json にテストユーザー/admin を事前登録、 CreateUser のブートストラップ順序
* 冪等性まわりのやり方を決める
  * 生成系: resource id をサーバ採番にし、 冪等は別のクライアント冪等キーで担保（1 つの値に兼任させない）
  * 冪等判定は **DCB（イベントストア）**で行う。 別テーブルにしない（ES のグレインに乗せる）
  * スコープ = 操作主体（sub）。 owner 集約のある配送先は user 境界で自然にスコープ（bare key）、 root の brand/product は `sub:key`
  * 衝突対策: principal スコープ + リクエストフィンガープリント（同キー×別本体は 422 で弾く）
  * createdBy: creator≠owner のもの（brand/product）だけ domain に持つ。 監査だけなら event metadata（CorrelationDataProvider）で横断記録
* 配送先のセキュリティリスク（要修正・優先）
  * register の id がクライアント採番 → 攻撃者が衝突 id を選べる + read model の PK が id 単独 + projector が `WHERE id` のみ → **cross-tenant の上書き/削除**が成立
  * 修正: 配送先 id を**サーバ採番**にする（攻撃者が id を選べなくする＝経路が閉じる）
  * 多層防御: projector の update/delete/changeDefault に `user_id` 条件を追加
