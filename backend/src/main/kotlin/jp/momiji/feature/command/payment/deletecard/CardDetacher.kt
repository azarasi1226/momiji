package jp.momiji.feature.command.payment.deletecard

import jp.momiji.event.payment.CardDeletedEvent
import jp.momiji.feature.command.InitialPosition
import jp.momiji.feature.command.pooledStreamingProcessorFor
import jp.momiji.port.payment.PaymentGateway
import org.axonframework.messaging.eventhandling.annotation.EventHandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component

/**
 * カード削除イベントを受けて、 Stripe 側の pm_ を Detach する外部副作用ハンドラ。
 *
 * 外部 IO なので非同期（pooledStreaming）。 Detach は冪等（[PaymentGateway.detachPaymentMethod] が不在系を握る）。
 * profile ゲートはしない（IdpUserDeleter と同じ流儀）: 統合テストでは PaymentGateway port が mock 供給される。
 */
@Component
class CardDetacher(
    private val paymentGateway: PaymentGateway,
) {
    @EventHandler
    fun on(event: CardDeletedEvent) {
        paymentGateway.detachPaymentMethod(event.paymentMethodId)
    }

    @Configuration
    class Config {
        @Bean
        fun cardDetacherProcessor() = pooledStreamingProcessorFor<CardDetacher>("card-detach", InitialPosition.LATEST)
    }
}
