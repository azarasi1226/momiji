package jp.momiji.feature

import org.axonframework.extension.spring.config.EventProcessorDefinition
import org.axonframework.messaging.eventhandling.processing.streaming.pooled.PooledStreamingEventProcessorConfiguration

/**
 * 単一の EventHandler クラス [T] を、 専用の subscribing(同期) Event Processor として登録する
 * [EventProcessorDefinition] を生成するヘルパ。
 *
 * 各 lookup projector の `@Configuration class Config` から
 * `@Bean fun xxxDefinition() = subscribingProcessorFor<XxxLookupProjector>()` の形で使う。
 *
 * - processor 名は [T] の simpleName 由来 → クラスをリネームすると processor 名も型安全に追従する
 *   (文字列で yaml に書くと黙って陳腐化する問題を避ける。 ADR 0007 と同じ動機)。
 * - `assigningHandlers` で 「 その processor が担当するのは [T] の bean だけ 」 と型で限定する。
 * - `notCustomized()` で 「 追加チューニングなし 」 を明示 (subscribing は同期実行でノブが無いため)。
 *
 * NOTE: Axon は `EventProcessorDefinition` 型の bean を `List` で型注入して集めるため、
 * `@Bean` の bean 名 (= メソッド名) は衝突さえしなければ何でもよい。 ただし Spring は
 * bean 名重複を `BeanDefinitionOverrideException` で弾く (Spring Boot 2.1+ の既定) ので、
 * 呼び出し側の `@Bean` メソッド名は projector ごとに固有にすること。
 */
inline fun <reified T : Any> subscribingProcessorFor(): EventProcessorDefinition =
    EventProcessorDefinition
        .subscribing(T::class.simpleName!!)
        .assigningHandlers { it.beanType() == T::class.java }
        .notCustomized()

/**
 * [pooledStreamingProcessorFor] の初期トークン位置（トークンが無い初回起動時のみ参照される）。
 */
enum class InitialPosition {
    /** ストリームの最新 (= 今) から。 過去を再生しない。 外部副作用ハンドラの安全側既定。 */
    LATEST,

    /** ストリームの先頭から (過去を全再生)。 read model 再構築など履歴が必要な用途。 */
    FIRST,
}

/**
 * 単一の EventHandler クラス [T] を、 専用の PooledStreaming(非同期) Event Processor として登録する
 * [EventProcessorDefinition] を生成するヘルパ。 外部副作用 (メール送信 / IdP 呼び出し等) のように
 * コマンド処理をブロックせず非同期で流したいハンドラ向け。
 *
 * - [processorName] は **安定した固定文字列** を渡す ([T] の simpleName から導出しない)。
 *   PooledStreaming は TokenStore に進捗トークンを **processor 名でキーして永続** するため、
 *   名前がコード構造に連動して変わると、 リネーム/パッケージ移動のたびに旧トークンを見失い、
 *   先頭から全再生 → 過去の副作用 (メール再送 / IdP 再呼び出し) を撃ち直す事故になる。
 *   この固定名は DLQ プロパティ (`axon.eventhandling.processors.<名前>.dlq.enabled`) のキーとも一致させる。
 * - 割り当ては `assigningHandlers` で [T] の bean 型に限定 (名前と違いリネームに追従して良い)。
 * - **初期位置は [initialPosition] で必ず明示する**（既定なし。 トークンが無い初回起動時のみ参照される）。
 *   [InitialPosition.LATEST] = 今から（過去を再生しない。 外部副作用の安全側）/ [InitialPosition.FIRST] =
 *   先頭から（過去全再生。 read model 再構築用）。 **安全な既定が無く**（副作用には LATEST、 read model には
 *   FIRST が正しく、 既定を置くと逆用途で黙って誤動作する）、 かつ失敗が**サイレント**（過去の副作用を撃ち直す
 *   or イベント取りこぼし）なため、 呼び出しごとに意識的に選ばせる。
 *   初期位置は **[customize] の後に強制適用**されるため、 customize が initialToken に触れても
 *   ( 触れ忘れても ) [initialPosition] が常に権威を持ち、 意図せず先頭再生に倒れない。
 * - エラー時は既定で **無限リトライ** (`PropagatingErrorHandler`: 例外を再 throw → セグメントを
 *   手放して再試行)。 「 詰まったら脇に避けて先へ進む 」 にしたい processor は AF5.1.1 では
 *   プログラム API でなく **プロパティ** `axon.eventhandling.processors.<名前>.dlq.enabled=true` で
 *   DLQ を有効化する (JDBC の DLQ テーブルが要る)。
 * - [customize] でセグメント数 (並列度) / batchSize / 自作 errorHandler 等を追加調整できる。
 */
inline fun <reified T : Any> pooledStreamingProcessorFor(
    processorName: String,
    initialPosition: InitialPosition,
    noinline customize: (PooledStreamingEventProcessorConfiguration) -> PooledStreamingEventProcessorConfiguration = { it },
): EventProcessorDefinition =
    EventProcessorDefinition
        .pooledStreaming(processorName)
        .assigningHandlers { it.beanType() == T::class.java }
        .customized { config ->
            // customize を先に、 初期位置は最後に「強制」適用する。 こうすれば customize が initialToken に
            // 触れても (触れ忘れても) initialPosition が常に権威を持ち、 意図せず先頭再生に倒れない。
            val customized = customize(config)
            when (initialPosition) {
                InitialPosition.LATEST -> customized.initialToken { source -> source.latestToken(null) }
                InitialPosition.FIRST -> customized.initialToken { source -> source.firstToken(null) }
            }
        }
