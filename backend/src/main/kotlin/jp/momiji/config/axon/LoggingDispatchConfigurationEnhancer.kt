package jp.momiji.config.axon

import org.axonframework.common.configuration.ComponentRegistry
import org.axonframework.common.configuration.ConfigurationEnhancer
import org.axonframework.messaging.core.interception.DispatchInterceptorRegistry
import org.springframework.stereotype.Component

/**
 * dispatch ログ用 interceptor（[LoggingCommandDispatchInterceptor] / [LoggingEventDispatchInterceptor]）を
 * Axon の dispatch チェーンに登録する [ConfigurationEnhancer]。
 *
 * AF5 では dispatch interceptor は [DispatchInterceptorRegistry] に登録する（旧 `commandBus.registerDispatchInterceptor` は廃止）。
 * 既存の registry コンポーネントを decorate し、 command 用・event 用の interceptor を足す。
 * Spring Boot starter が context 内の [ConfigurationEnhancer] Bean を自動収集して Axon 設定に適用する
 * （統合テストの `MessagesRecordingConfigurationEnhancer` と同じ仕組み）。
 */
@Component
class LoggingDispatchConfigurationEnhancer : ConfigurationEnhancer {
    override fun enhance(registry: ComponentRegistry) {
        registry.registerDecorator(
            DispatchInterceptorRegistry::class.java,
            DECORATION_ORDER,
        ) { _, _, delegate ->
            delegate
                .registerCommandInterceptor { _ -> LoggingCommandDispatchInterceptor() }
                .registerEventInterceptor { _ -> LoggingEventDispatchInterceptor() }
        }
    }

    private companion object {
        // 既定の interceptor 群への足し込み。 ログは順序非依存なので 0 で十分（明示しておく）。
        private const val DECORATION_ORDER = 0
    }
}
