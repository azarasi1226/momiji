package jp.momiji.config.axon

import io.github.oshai.kotlinlogging.KotlinLogging
import org.axonframework.messaging.core.MessageDispatchInterceptor
import org.axonframework.messaging.core.MessageDispatchInterceptorChain
import org.axonframework.messaging.core.MessageStream
import org.axonframework.messaging.core.unitofwork.ProcessingContext
import org.axonframework.messaging.eventhandling.EventMessage

private val logger = KotlinLogging.logger {}

/**
 * イベント発行（dispatch/publish）を横断的にログする AF5 の [MessageDispatchInterceptor]。
 * 「どんなイベントが publish されているか」を見るための **開発時の可観測性**用。
 *
 * コマンド版（[LoggingCommandDispatchInterceptor]）と同じ方針で **DEBUG のみ**。 ES なので append される全イベントが
 * ここを通る（量が多い）。 本番の可視化はトレーシング（Tempo/OpenTelemetry）の責務。 payload は PII を含みうるが DEBUG 限定。
 *
 * 登録は [LoggingDispatchConfigurationEnhancer] が [org.axonframework.messaging.core.interception.DispatchInterceptorRegistry] に対して行う。
 */
class LoggingEventDispatchInterceptor : MessageDispatchInterceptor<EventMessage> {
    override fun interceptOnDispatch(
        message: EventMessage,
        context: ProcessingContext?,
        chain: MessageDispatchInterceptorChain<EventMessage>,
    ): MessageStream<*> {
        logger.debug {
            "イベント発行: ${message.payloadType().simpleName} " +
                "(id=${message.identifier()}, type=${message.type()}) payload=${message.payload()}"
        }
        return chain.proceed(message, context)
    }
}
