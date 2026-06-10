# ADR 0008: イベントに日時を持たせず、プロジェクションは `@Timestamp` から日時を取得する

- **ステータス**: 採用 ( `UserTableProjector` の created_at / updated_at を `@Timestamp` 由来に変更済み )
- **作成日**: 2026-06-03
- **関連 ADR**: [0007 イベント型名をパッケージ構造から分離する](./0007-イベント型名をパッケージ構造から分離する.md)（どちらもイベントストア / 再生の堅牢性に関する判断）

## コンテキスト

momiji は CQRS + Event Sourcing（Axon 5.1.1 DCB）。read model（`users` テーブル等）は `@EventHandler` を持つ projector がイベントを受けて構築する。read model には `created_at` / `updated_at` のような日時カラムがある。

Event Sourcing の核心的な利点の 1 つは、**イベントを再生して read model をいつでも再構築できる**こと。したがって projector は「再生しても同じ結果になる（決定的である）」必要がある。

### 経緯 ( なぜ ADR にするか )

当初 `UserTableProjector` は日時に **`LocalDateTime.now()`**（＝射影が実行された時刻）を入れていた。これは**プロジェクション再構築（リプレイ）に対して非安全**:

- read model を作り直すと、全行の `created_at` / `updated_at` が**「再構築した瞬間の時刻」に書き換わる**
- 「いつ作成/更新されたか」という履歴がリプレイで壊れる

read model の日時は「射影が走った時刻」ではなく「**イベントが起きた時刻**」由来であるべき、という原則を明文化する。

## 決定

1. **イベントのペイロードに「技術的な発生時刻」を持たせない。** イベントがイベントストアに保存された時刻は、フレームワークが `EventMessage` のメタデータとして既に記録しているため、ペイロードに重複して持たせない。
2. **projector で日時が必要なときは Axon の `@Timestamp` で取得する。** `@Timestamp` はイベントが**保存された時刻（append 時に確定し永続化される値）**を注入する。`@EventHandler` の実行時刻ではない。

```kotlin
@EventHandler
fun on(
    event: UserCreatedEvent,
    @Timestamp timestamp: Instant,
) {
    val at = LocalDateTime.ofInstant(timestamp, ZoneId.systemDefault())
    dsl.insertInto(USERS)
        .set(USERS.CREATED_AT, at)
        .set(USERS.UPDATED_AT, at)
        // ...
        .execute()
}
```

## なぜ必要か ( この ADR の核心 )

### `now()` と `@Timestamp` の決定的な違い

| | `LocalDateTime.now()` | `@Timestamp` |
|---|---|---|
| 値の意味 | **射影が実行された時刻** | **イベントが保存された時刻**（永続値） |
| 通常運用 | ほぼイベント発生時刻（射影ラグ分ずれる） | イベント発生時刻 |
| リプレイ / 再構築 | **全行が再構築時刻に書き換わる**（履歴破壊） | 各イベントの**元の過去時刻が再現**される（決定的） |

read model は「イベントから導出される投影」であり、同じイベント列からは**何度再構築しても同じ結果**になるべき。`now()` はこの決定性を壊すが、`@Timestamp` は保つ。

### なぜイベントに日時フィールドを足さないのか

「ペイロードに `occurredAt` を持たせればよいのでは」とも考えられるが、**イベントストアが append 時刻を既に持っている**ため重複になり、両者が食い違うリスクが生まれる。技術的な発生時刻は**メタデータ（`@Timestamp`）から取るのが単一の真実源**になる。

## 妥協点 / 検討した代替案

### 代替: イベントに `occurredAt` フィールドを持たせる ( 原則は不採用 )

技術的な「発生時刻」については不採用（上記理由）。ただし **次の場合はイベントのペイロードに明示的な日時フィールドを持たせる**:

- **ドメイン上意味のある時刻**が、技術的な append 時刻と異なるとき（例: バックデートされた有効日、業務上の基準時刻、外部システムが報告した発生時刻 等）

つまり区別する:

- **「いつこのイベントが記録されたか」= 技術的時刻** → `@Timestamp`（ペイロードに持たせない）
- **「業務的にいつのことか」= ドメイン時刻** → イベントの明示フィールド

### タイムゾーン

`@Timestamp` は `Instant`（UTC 基準の時点）。read model の `datetime` カラムへは zone を決めて `LocalDateTime` に変換する。現状は既存挙動を変えないため `ZoneId.systemDefault()` を使用。将来 UTC 固定に寄せる余地はある（その場合は表示側の時刻がずれる点に注意）。

## 帰結

良かった点:

- read model が**リプレイで決定的に再構築可能**になり、Event Sourcing の利点（再構築・別 read model の追加構築）を安全に使える
- 技術的発生時刻の真実源が `@Timestamp` に一本化され、ペイロードとの食い違いが起きない

注意すべき点:

- projector で日時を扱うときは **`now()` を使わない**。必ず `@Timestamp`（または明示的なドメイン日時フィールド）を使う
- 新しい projector / read model を足すときも同じ原則を適用する
- `Instant → LocalDateTime` の zone 変換に依存があるため、TZ 方針（systemDefault / UTC）はプロジェクト内で揃える

## 関連

- ソースコード: [UserTableProjector](../../backend/src/main/kotlin/jp/momiji/projection/user/UserTableProjector.kt)
- Axon: `org.axonframework.messaging.eventhandling.annotation.Timestamp`（`EventMessage` の保存時刻を注入）
