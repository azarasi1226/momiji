# ADR 0009: Event Processor の構成をコードに型安全に定義し、subscribing / PooledStreaming で命名規約を分ける

- **ステータス**: 採用 ( `subscribingProcessorFor` / `pooledStreamingProcessorFor` を導入し、 lookup projector と外部副作用ハンドラに適用済み )
- **作成日**: 2026-06-04
- **関連 ADR**: [0007 イベント型名をパッケージ構造から分離する](./0007-イベント型名をパッケージ構造から分離する.md)（型/トークンの identity をコード構造から切り離すという同じ動機）

## コンテキスト

momiji の backend は Axon Framework 5.1.1（DCB）の CQRS + Event Sourcing。 `@EventHandler` を持つクラスは Event Processor に束ねられて実行される。 Axon 5.1.1（axon-spring の `EventProcessorDefinition`）では、 各 processor の **モード（subscribing / PooledStreaming）・名前・担当ハンドラ・チューニング**を `@Bean` として宣言的に登録できる。 yaml（`axon.eventhandling.processors.*`）でも一部設定できる。

momiji の `@EventHandler` クラスは役割で 3 種類ある:

| 役割 | 例 | 望ましいモード |
|---|---|---|
| lookup テーブル保守（コマンド側の一意性チェック用） | `CreateUserLookupProjector` 等 | **subscribing**（同期。 コマンドの UoW 内で確定させたい） |
| read model 投影 | `UserTableProjector` | streaming（非同期・再構築可能） |
| 外部副作用（メール送信 / IdP 呼び出し） | `EmailToIdpSyncer`, `IdpUserDeleter` 等 | **PooledStreaming**（非同期。 コマンドをブロックしない） |

### 経緯 ( なぜ ADR にするか )

processor 設定を yaml の文字列名で持つと、 **processor 名がコード構造（クラス名・パッケージ）と二重管理**になり、 リネームで黙って陳腐化する。 これは [ADR 0007](./0007-イベント型名をパッケージ構造から分離する.md) でイベント型名について踏んだのと同じ罠。

さらに PooledStreaming 特有の落とし穴がある。 PooledStreaming は **進捗トークンを TokenStore に「processor 名」でキーして永続**する。 processor 名をクラス名由来にすると、 クラスのリネーム / パッケージ移動で名前が変わった瞬間に旧トークンを見失い、 **ストリーム先頭から全イベントを再生**する。 対象が外部副作用（メール / IdP）なら、 **過去分のメールを全部撃ち直す / IdP を全部叩き直す事故**になる。

加えて検証の結果、 Axon 5.1.1 では:

- PooledStreaming の **初期トークンの既定は `firstToken`（ストリーム先頭）**。 新規 processor は初回起動でトークンが無いと過去を全再生する。
- `ErrorHandler` 実装は `PropagatingErrorHandler` のみ（再 throw → セグメントを手放して再試行 = **実質無限リトライ**）。 「ログして握って進む」 実装は標準では無い。
- **Dead Letter Queue（DLQ）はプログラム API に無く、 プロパティ駆動**（`axon.eventhandling.processors.<名前>.dlq.enabled` + JDBC/JPA autoconfig）。 これも **processor 名でキー**される。

これらの判断（どこで設定するか、 名前をどう決めるか、 初期位置・エラー方針の既定）を将来ブレさせないため明文化する。

## 決定

### 1. processor 構成はコードに型安全に定義する（yaml の文字列名にしない）

`EventProcessorDefinition` を返すヘルパ [`feature/EventProcessorDefinitions.kt`](../../backend/src/main/kotlin/jp/momiji/feature/EventProcessorDefinitions.kt) を用意し、 各ハンドラの `@Configuration class Config` から `@Bean` で登録する。 担当ハンドラの割り当ては **bean 型**で行う（`assigningHandlers { it.beanType() == T::class.java }`）。 これはリファクタに自動追従してよい。

### 2. subscribing と PooledStreaming で命名規約を「逆」にする

```kotlin
// subscribing: 名前はクラスの simpleName 由来でよい
inline fun <reified T : Any> subscribingProcessorFor(): EventProcessorDefinition

// PooledStreaming: 名前は呼び出し側が「安定した固定文字列」を渡す（simpleName から導出しない）
inline fun <reified T : Any> pooledStreamingProcessorFor(
    processorName: String,
    initialPosition: InitialPosition,   // 必須（既定なし）
    noinline customize: (PooledStreamingEventProcessorConfiguration) -> PooledStreamingEventProcessorConfiguration = { it },
): EventProcessorDefinition
```

| | subscribing | PooledStreaming |
|---|---|---|
| トークン | 無し（同期実行） | **有り**（TokenStore に processor 名でキーして永続） |
| processor 名 | **クラス simpleName 由来でよい** | **安定した固定文字列を明示**（コード構造から独立） |
| 名前を変えると | 影響なし（ただのラベル） | 旧トークン喪失 → 先頭から全再生（事故） |

固定名の一覧（一度決めたら不変の契約）:

| ハンドラ | 固定 processor 名 |
|---|---|
| `EmailToIdpSyncer` | `email-to-idp-sync` |
| `IdpUserDeleter` | `idp-user-delete` |
| `OldEmailChangeNotifier` | `old-email-change-notify` |
| `EmailChangeEmailSender` | `email-change-mail-send` |

### 3. PooledStreaming の初期位置は専用引数 `initialPosition` で **必須**（既定なし）

`pooledStreamingProcessorFor` は `initialPosition: InitialPosition`（`LATEST` = `latestToken` = 今から / `FIRST` = `firstToken` = 先頭から）を**必須引数**として取る（既定値を置かない）。 **安全な既定が存在しない**ため: 外部副作用には `LATEST`（過去の副作用を撃ち直さない）、 read model 再構築には `FIRST`（履歴を全再生）が正しく、 **どちらを既定にしても逆用途で黙って誤動作**する。 しかも失敗は**サイレント**（過去の副作用撃ち直し or イベント取りこぼしで、 コンパイルも起動も通ってしまう）。 サイレントで重大な判断なので、 呼び出しごとに意識的に選ばせる。 副作用は `LATEST`、 read model は `FIRST` を明示する。

初期位置を `customize` ラムダに埋め込まず**専用引数にして `customize` の後に強制適用**するのが要点。 `customize` の中に初期位置を混ぜると、 呼び出し側が `customize` で initialToken に触れた（または触れ忘れた）拍子に **意図せず先頭再生（firstToken）に倒れる**実装上の脆さが残る。 `initialPosition` を最後に適用すれば、 `customize` が何をしても初期位置は常に引数が権威を持つ。 なお `initialToken` は **トークンが無い初回起動時のみ参照**され、 2 回目以降は TokenStore の永続トークンから再開する（ tuning を変えて再デプロイしても再生は起きない ）。

### 4. エラー方針: 無限リトライが既定、 DLQ はプロパティで opt-in

`pooledStreamingProcessorFor` は既定で無限リトライ（`PropagatingErrorHandler`）。 「詰まったら脇に避けて先へ進む」 にしたい processor は **プロパティ**で DLQ を有効化する。 キーは固定 processor 名と一致させる:

```yaml
axon:
  eventhandling:
    processors:
      email-to-idp-sync:
        dlq:
          enabled: true   # JDBC の DLQ テーブルが要る
```

## なぜ必要か ( この ADR の核心 )

### identity をコード構造から切り離す（ADR 0007 と同じ精神）

ADR 0007 は「イベントの wire 型名」を、 本 ADR は「PooledStreaming のトークン身元（= processor 名）」を、 それぞれコードの物理構造から切り離す。 どちらも **「内部実装の都合であるべきリファクタ（リネーム・移動）が、 永続化された状態の互換性を黙って壊す」** という同型の罠への対策。 違いは:

- **subscribing はトークンを持たない**ので名前が identity にならない。 だからクラス名由来で問題なく、 むしろリネーム追従して便利。
- **PooledStreaming はトークンを名前でキーする**ので名前が identity になる。 だから固定名で「コード構造と無関係に不変」 にしておく必要がある。

「名前と担当割り当ての分離」 が鍵。 担当は bean 型（構造的・リファクタ追従）、 名前は固定文字列（identity・不変）。 これで「クラスは自由にリネーム / 移動できるが、 トークンの身元は動かない」 を両立する。

### 構造はコード、 環境依存のチューニングはプロパティ

モード・担当・初期位置・エラー方針といった **構造的な意思決定はコード**で型安全に持つ。 一方 DLQ 有効化やそのキャッシュ設定、 将来のセグメント数（並列度）など **環境（local / prod）で変えたい運用ノブはプロパティ**が適所。 DLQ が AF5.1.1 でプロパティ駆動なのは、 この住み分けと整合する。

## 妥協点 / 検討した代替案

### 代替 A: processor を yaml で設定する ( 不採用 )

全 processor の一覧性は yaml が勝る。 しかし processor 名が文字列になり、 クラス / パッケージのリネームで黙って陳腐化する（ADR 0007 の罠の再来）。 また subscribing はノブが無く、 PooledStreaming も担当割り当てを型で書けない。 構造はコード、 運用ノブだけプロパティ、 に倒した。

### 代替 B: PooledStreaming の名前も simpleName 由来にする ( 不採用 )

subscribing と同じ書き味で揃うが、 リネームでトークンを喪失して全再生する。 外部副作用では致命的。 PooledStreaming だけは固定名を必須引数にして、 「名前を考えてコミットする」 ことを強制した。

### 代替 C: DLQ をプログラム API で設定する ( 不可 )

AF5.1.1 の `EventProcessorDefinition` / `PooledStreamingEventProcessorConfiguration` に DLQ の口は無く、 プロパティ + autoconfig 経由でしか構成できない。 ヘルパに `dlq: Boolean` 引数を生やすのは不可能なので、 プロパティ駆動を正式な手段とした。

## 帰結

良かった点:

- processor のモード・担当・初期位置がコードに型安全に書かれ、 クラスのリネーム / 移動が安全（トークンを喪失しない）
- 外部副作用ハンドラが **初回起動で過去を撃ち直さない**（`latestToken` 既定）
- 「無限リトライ = コード既定 / DLQ = プロパティ opt-in」 の住み分けが明確
- subscribing（lookup projector）と PooledStreaming（副作用）の差が命名規約に現れ、 意図が読める

注意すべき点 ( 重要 ):

- **PooledStreaming の固定名は後方互換の契約**。 一度決めたら変えない（変えるとトークンを喪失して全再生）。 クラスをリネームしても固定名は触らないこと
- read model の `UserTableProjector` は現状 **パッケージ名由来の既定名**（`jp.momiji.projection.user`）の PooledStreaming で動いている。 これは ADR 0007 / 本 ADR が警告する「パッケージ移動でトークン喪失 → 全再生」 のリスクそのもの。 安定名を明示する余地がある（未対応・フォローアップ）
- DLQ を有効化する processor は JDBC の DLQ テーブルが必要（`JdbcDeadLetterQueueAutoConfiguration`）。 enable と同時にスキーマを用意すること
- subscribing は同期実行のため、 重い / 外部 I/O を伴うハンドラを subscribing にするとコマンド処理をブロックする。 外部副作用は PooledStreaming にする（本 ADR の適用方針）

## 関連

- ソースコード:
  - [feature/EventProcessorDefinitions.kt](../../backend/src/main/kotlin/jp/momiji/feature/EventProcessorDefinitions.kt)（`subscribingProcessorFor` / `pooledStreamingProcessorFor`）
  - lookup projector（subscribing）: [CreateUserLookupProjector](../../backend/src/main/kotlin/jp/momiji/feature/user/create/CreateUserLookupProjector.kt) / [DeleteUserLookupProjector](../../backend/src/main/kotlin/jp/momiji/feature/user/delete/DeleteUserLookupProjector.kt) / [ConfirmEmailChangeLookupProjector](../../backend/src/main/kotlin/jp/momiji/feature/user/changeemail/confirm/ConfirmEmailChangeLookupProjector.kt)
  - 外部副作用（PooledStreaming）: [EmailToIdpSyncer](../../backend/src/main/kotlin/jp/momiji/feature/user/changeemail/confirm/EmailToIdpSyncer.kt) / [IdpUserDeleter](../../backend/src/main/kotlin/jp/momiji/feature/user/delete/IdpUserDeleter.kt) / [OldEmailChangeNotifier](../../backend/src/main/kotlin/jp/momiji/feature/user/changeemail/confirm/OldEmailChangeNotifier.kt) / [EmailChangeEmailSender](../../backend/src/main/kotlin/jp/momiji/feature/user/changeemail/request/EmailChangeEmailSender.kt)
  - TokenStore スキーマ作成: [AxonConfig](../../backend/src/main/kotlin/jp/momiji/config/AxonConfig.kt)
- Axon:
  - `org.axonframework.extension.spring.config.EventProcessorDefinition`（`subscribing` / `pooledStreaming` / `assigningHandlers` / `customized`）
  - `org.axonframework.messaging.eventhandling.processing.streaming.pooled.PooledStreamingEventProcessorConfiguration`（`initialToken` / `errorHandler` / `initialSegmentCount` / `batchSize` 等）
  - `org.axonframework.messaging.eventstreaming.TrackingTokenSource`（`firstToken` = 先頭 / `latestToken` = 今 / `tokenAt`）
  - `org.axonframework.messaging.eventhandling.processing.errorhandling.PropagatingErrorHandler`（既定 = 無限リトライ）
  - DLQ: `io.axoniq.framework.springboot.DeadLetterQueueProcessorProperties`（`axon.eventhandling.processors.<名前>.dlq.*`）+ `JdbcDeadLetterQueueAutoConfiguration`
