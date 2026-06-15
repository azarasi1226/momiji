package jp.momiji.config.axon

import io.github.oshai.kotlinlogging.KotlinLogging
import org.axonframework.messaging.commandhandling.CommandMessage
import org.axonframework.messaging.core.MessageDispatchInterceptor
import org.axonframework.messaging.core.MessageDispatchInterceptorChain
import org.axonframework.messaging.core.MessageStream
import org.axonframework.messaging.core.unitofwork.ProcessingContext

private val logger = KotlinLogging.logger {}

/**
 * コマンド発行（dispatch）を横断的にログする AF5 の [MessageDispatchInterceptor]。
 * 「どんなコマンドが送られているか」を見るための **開発時の可観測性**用。
 *
 * **DEBUG のみ**で出す。 本番のコマンド可視化はトレーシング（Tempo/OpenTelemetry）の責務で、 全コマンドを INFO で
 * 流すのはノイズなので避ける。 payload は住所・氏名等の PII を含みうるが、 DEBUG 限定なので 1 行に id・種別とまとめて出す。
 * 必要なら logback で `jp.momiji.config.axon` を DEBUG にするか、 actuator の loggers で本番でも一時的に上げて調べられる。
 *
 * 登録は [LoggingDispatchConfigurationEnhancer] が [org.axonframework.messaging.core.interception.DispatchInterceptorRegistry] に対して行う。
 */
class LoggingCommandDispatchInterceptor : MessageDispatchInterceptor<CommandMessage> {
    override fun interceptOnDispatch(
        message: CommandMessage,
        context: ProcessingContext?,
        chain: MessageDispatchInterceptorChain<CommandMessage>,
    ): MessageStream<*> {
        logger.debug {
            "コマンド発行: ${message.payloadType().simpleName} " +
                "(id=${message.identifier()}, type=${message.type()}) payload=${message.payload()}"
        }
        return chain.proceed(message, context)
    }
}
