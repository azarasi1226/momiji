package jp.momiji.feature

import org.axonframework.extension.spring.config.EventProcessorDefinition

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
