package jp.momiji.feature.command.order.cancel

import jp.momiji.event.order.OrderCancelledEvent
import jp.momiji.feature.command.InitialPosition
import jp.momiji.feature.command.pooledStreamingProcessorFor
import jp.momiji.port.payment.PaymentGateway
import org.axonframework.messaging.eventhandling.annotation.EventHandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component

/**
 * 注文キャンセルイベントを受けて、 課金確定済み（PAID）注文の決済を Stripe で返金する外部副作用ハンドラ。
 *
 * 外部 IO なので非同期（pooledStreaming・LATEST）。 返金対象は [OrderCancelledEvent.refundPaymentIntentId] が
 * 載っているとき（＝キャンセル時点で PAID）だけ。 未課金キャンセルは pi_ が null なので何もしない。
 * 返金は冪等（[PaymentGateway.refundPayment] が「既に返金済み」等の恒久エラーを握り、 一時障害は投げてリトライに乗せる）。
 * profile ゲートはしない（CardDetacher と同じ流儀。 統合テストでは PaymentGateway port が mock 供給される）。
 */
@Component
class OrderRefunder(
    private val paymentGateway: PaymentGateway,
) {
    @EventHandler
    fun on(event: OrderCancelledEvent) {
        val paymentIntentId = event.refundPaymentIntentId ?: return
        paymentGateway.refundPayment(paymentIntentId)
    }

    @Configuration
    class Config {
        @Bean
        fun orderRefunderProcessor() = pooledStreamingProcessorFor<OrderRefunder>("order-refund", InitialPosition.LATEST)
    }
}
