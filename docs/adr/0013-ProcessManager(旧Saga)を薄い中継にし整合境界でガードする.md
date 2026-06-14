# ADR 0013: Process Manager（旧 Saga）を薄い中継にし、 ガードは整合境界（CommandHandler）に置く

- **ステータス**: 採用（注文機能の Process Manager 設計方針として確定。 実装はこれから）
- **作成日**: 2026-06-14
- **関連 ADR**: [0009 EventProcessor の構成をコードに型安全に定義する](./0009-EventProcessorの構成をコードに型安全に定義する.md)（初期位置 LATEST / FIRST の判断はここに従う）、 [0007 イベント型名をパッケージ構造から分離する](./0007-イベント型名をパッケージ構造から分離する.md)（再生が壊れない型名の固定）
- **関連ドキュメント**: [注文オーケストレーションフロー（たたき台）](../設計/注文機能たたき台.md)

## コンテキスト

momiji は Axon Framework 5.1.1（DCB）。 AF5 では **Saga / Aggregate は legacy 化し、 DCB + Process Manager（PM）が推奨**。 注文機能はこの PM を本格的に使う最初の題材になる。

注文フローは長期実行のオーケストレーション（[注文たたき台](../設計/注文機能たたき台.md)）。 後半レーンで PM が「支払い成功イベント → かご削除コマンド ＋ 発送依頼コマンド」「発送完了 → 在庫確定コマンド」「支払い失敗 → 確保解放コマンド」のように、 **イベントを受けてコマンドを撃つ**。

ここで重要なのは、 **PM は副作用を伴う reactor** だということ。 PM が撃つコマンドは最終的に「実際の発送・課金・メール送信」という現実の副作用につながる。 read model 投影（純粋な状態書き込み・冪等 upsert）とは性質が違う。

### 論点（なぜ ADR にするか）

1. **新しい reactor を後から足すと、 履歴 replay で過去オーダーに副作用が走る**。 PM の processor をストリーム先頭から起動すると、 半年前の `OrderPlaced` を replay → 途中ステップのコマンドを撃つ → **過去の完了済みオーダーを今ごろ発送する**事故になる。 `@EndSaga` 相当の完了イベントに到達するのは replay の後なので、 ライフサイクルでは止められない。
2. **進行中のオーダーだけに反応させたい**（遅延・重複・順序前後したイベントへの堅牢性）。
3. **旧 Saga とどう違うか**を明文化したい。 旧 Saga は「saga store に自分の状態を持ち、 コマンドを撃つ前にガード（guard → fire）」だった。 DCB ではこれを反転させる。

これらの判断を将来ブレさせないため明文化する。

## 決定

### 1. PM は「イベント → コマンド」の薄い中継にする（状態・不変条件を持たせない）

PM 自身に process 状態や不変条件を持たせない（旧 Saga の saga store のような **第 2 の真実源を作らない**）。 PM の責務は「このイベントを受けたら、 このコマンドを撃つ」というルーティングだけ。

### 2. authoritative なガードは PM ではなく、 PM が撃つ CommandHandler に置く（fire → guard）

不変条件（「PAID のオーダーだけ発送できる」等）は、 PM が撃った先の **CommandHandler が DCB sourcing で守る**。 momiji は元々「CommandHandler が state を source して不変条件を守る」流儀なので、 これに乗せる。

```
PM: 支払い成功イベントを受信 → 発送依頼コマンドを撃つ（PM はここまで）
        │
        ▼
発送依頼 CommandHandler: ORDER_ID で order 状態を source
  - 期待状態（PAID）でなければ reject
```

これにより 1 つのガードが 3 役を兼ねる:

- **進行中判定**: PAID のオーダーにだけ作用
- **冪等性**: 同じイベントが 2 回来ても、 2 回目は既に状態が進んでいて reject
- **順序前後 / 遅延への堅牢性**: 状態が合わなければ reject

### 3. PM を載せる processor は reactor なので `LATEST` 起動（ADR 0009 準拠）

PM の `@EventHandler` は外部副作用ハンドラと同じ reactor。 [ADR 0009](./0009-EventProcessorの構成をコードに型安全に定義する.md) の方針どおり **PooledStreaming + `InitialPosition.LATEST`** で登録する（`pooledStreamingProcessorFor(name, InitialPosition.LATEST)`）。 これで新規 deploy 時に履歴を再生せず、 **過去オーダーに対してコマンドを撃たない**。 processor 名は固定文字列にする（リネームでトークンを喪失して全再生する事故を避ける。 ADR 0009 と同じ理由）。

### 4. 進行中判定・冪等・順序堅牢は「専用テーブル」でなく ORDER_ID の DCB sourcing で実現する

「進行中オーダーの一覧テーブルを持って EventHandler の先頭で照合」 という作りは採らない。 注文の横断イベント（支払い・発送・在庫）は `@EventTag(ORDER_ID)` を持つので、 **order の状態はイベントから source できる**。 決定 2 のガードはこの sourcing で行う（別テーブルを維持しない）。

### 5. CommandHandler を経由しない副作用（メール送信等）は別途「冪等 ＋ LATEST」で守る

決定 2 のガードは「コマンドを通る副作用」しか守れない。 メール送信のように **CommandHandler を経由しない副作用**は、 (a) `LATEST` 起動で履歴再生を避け、 (b) 規約どおり冪等に実装する（momiji 規約「Event Handler の外部副作用は冪等に」）。

## なぜ必要か（この ADR の核心）

### projection（replay 安全）と reactor（replay 不可）を分ける

- **projection** は純粋な状態書き込み。 何度 replay しても結果が同じ → 自由に再構築してよい。
- **reactor（PM・メール・コマンド発行）** は外部副作用。 replay = 過去の発送・課金・送信を **もう一度現実に実行**してしまう → replay 不可。

PM は後者。 だから「履歴で動かさない（`LATEST`）」が必須になる。 ここは DCB の機能ではなく **processor の初期位置の話**（ADR 0009 の領域）であることに注意。

### 唯一の真実源（イベントストア）を保つ

旧 Saga は saga store に状態を持っていた＝ **第 2 の真実源**で、 イベントストアと乖離しうる。 DCB で `ORDER_ID` から source すれば源は 1 つに保たれる。

### どこ発のコマンドでも守れる

ガードを PM（送信側）に置くと、 **PM が撃つコマンドしか守れない**。 リトライ・別経路・運用ツールから同じコマンドが来たら素通り。 **整合境界（CommandHandler）に置けば、 発信元が誰でも弾ける**。 これが「不変条件は整合境界に 1 か所集約」 という DCB の思想。

### deploy 跨ぎのオーダーを落とさない

`LATEST` 起動だと、 deploy 直前に `OrderPlaced` した進行中オーダーは、 PM が `OrderPlaced` を取り逃し、 後から来る `支払い成功` だけ受け取る。

- **専用 in-progress テーブル方式**だと、 テーブルに orderId が無く弾かれて **そのオーダーが詰む**。
- **ORDER_ID の DCB sourcing 方式**なら、 コマンド処理時に過去の `OrderPlaced` を含め全部 source するので、 token が `LATEST` でも状態を正しく復元でき、 **跨ぎオーダーを落とさない**。

これが「専用テーブルより ORDER_ID sourcing」を選ぶ決め手。

## 旧 Saga との対比

| | 旧 Saga（guard → fire） | DCB の PM（fire → guard） |
|---|---|---|
| 状態の源 | saga store（**第 2 の真実源**） | **イベントストア（ORDER_ID source）が唯一の源** |
| ガードの場所 | saga 自身（撃つ前に判断） | **CommandHandler（撃った後、 整合境界で判断）** |
| 乖離リスク | saga store と event store がずれうる | 源が 1 つなので乖離しない |
| 守れる範囲 | **saga が撃つコマンドだけ** | **どの発信元のコマンドでも** |
| 履歴 replay | 同じ問題（初期位置を head にして回避していた） | `LATEST` 起動で回避（ADR 0009） |

旧 Saga も「新規 deploy で履歴 replay → 過去で副作用」 は同じ問題を抱えており、 初期トークンを head にして避けていた。 本 ADR の `LATEST` 起動はその継承であって、 PM 特有の新問題ではない。

## 妥協点 / 検討した代替案

### 代替 A: PM に状態を持たせ「撃つ前にガード」（旧 Saga 型） — 不採用

PM 内で完結して一見素直だが、 (1) saga store という第 2 の真実源を持つ、 (2) PM 発以外のコマンドを守れない。 DCB の「唯一の源 ＋ 整合境界でガード」 と噛み合わないため不採用。 PM 側で source して「無駄打ちを間引く」 のは **任意の最適化**として許容するが、 authoritative なガードは必ず CommandHandler に置く。

### 代替 B: 進行中 order_id の専用テーブルを維持し EventHandler 先頭で照合 — 不採用

意図は正しいが、 (1) deploy 跨ぎオーダーで詰む（上述）、 (2) イベントストアと別の状態を手で維持する手間と乖離リスク。 同じ判定を ORDER_ID の DCB sourcing で行えば両方解決するため不採用。

### 代替 C: PM の processor を `FIRST`（先頭）起動 — 不可

履歴を全再生して過去オーダーにコマンドを撃つ（＝過去発送・過去課金）。 reactor では致命的。 ADR 0009 のとおり reactor は `LATEST` 一択。

## 帰結

良かった点:

- PM が薄い中継になり、 不変条件が整合境界（CommandHandler）に 1 か所集約される
- ガードが「進行中判定・冪等・順序堅牢」 を 1 つで兼ねる
- 専用 in-progress テーブルが不要（ORDER_ID sourcing）で、 deploy 跨ぎオーダーも落とさない
- 新しい reactor を後から足しても、 `LATEST` 起動で過去オーダーに副作用が走らない

注意すべき点（重要）:

- **`LATEST` 起動と「コマンド側ガード」は役割が違うので両方必要**。 前者は「履歴で動かさない（ストーム・非コマンド副作用の防止）」、 後者は「live の正しさ（二重・順序・状態）」。 片方では足りない。
- **CommandHandler を経由しない副作用（メール等）はコマンドガードで守れない**。 `LATEST` ＋ 冪等で個別に守る。
- **PM の状態を replay で作り直したくなったら、 replay 中はコマンド発行を抑止する**こと（素朴に再生すると副作用が再発火する）。
- ガードのために order の状態を source する CommandHandler は、 `ORDER_ID` タグの横断イベントを `@EventCriteriaBuilder` で集めて `@EventSourced` state に畳む（[UserShippingAddressState](../../backend/src/main/kotlin/jp/momiji/feature/command/user/shippingaddress/UserShippingAddressState.kt) が user_id 境界で行っているのと同型を、 order_id 境界で行う）。

## 関連

- 設計: [注文オーケストレーションフロー（たたき台）](../設計/注文機能たたき台.md)（PM2 / PM3 / PMF と ORDER_ID タグ / ライフサイクル 5 状態）
- ADR: [0009 EventProcessor 構成（初期位置 LATEST/FIRST）](./0009-EventProcessorの構成をコードに型安全に定義する.md) / [0007 イベント型名の固定](./0007-イベント型名をパッケージ構造から分離する.md)
- DCB sourcing の実装手本: [UserShippingAddressState](../../backend/src/main/kotlin/jp/momiji/feature/command/user/shippingaddress/UserShippingAddressState.kt)（`@EventSourced` / `@EventCriteriaBuilder` / `@InjectEntity` でタグ境界の state を source）
- processor 登録ヘルパ: [feature/EventProcessorDefinitions.kt](../../backend/src/main/kotlin/jp/momiji/feature/EventProcessorDefinitions.kt)（`pooledStreamingProcessorFor(name, InitialPosition.LATEST)`）
