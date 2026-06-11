package jp.momiji.feature.command.payment.deletecard

import jp.momiji.event.payment.CardDeletedEvent
import jp.momiji.feature.command.InitialPosition
import jp.momiji.feature.command.pooledStreamingProcessorFor
import jp.momiji.port.payment.PaymentGateway
import org.axonframework.messaging.eventhandling.annotation.EventHandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

/**
 * カード削除イベントを受けて、 Stripe 側の pm_ を Detach する外部副作用ハンドラ。
 *
 * 外部 IO なので非同期（pooledStreaming）。 Detach は冪等（[PaymentGateway.detachPaymentMethod] が不在系を握る）。
 * Stripe 実装が無い profile（統合テスト等）では起動しないよう `payment-stripe` でゲートする。
 */
@Component
@Profile("payment-stripe")
class CardDetacher(
    private val paymentGateway: PaymentGateway,
) {
    @EventHandler
    fun on(event: CardDeletedEvent) {
        paymentGateway.detachPaymentMethod(event.paymentMethodId)
    }

    @Configuration
    @Profile("payment-stripe")
    class Config {
        @Bean
        fun cardDetacherProcessor() = pooledStreamingProcessorFor<CardDetacher>("card-detach", InitialPosition.LATEST)
    }
}
