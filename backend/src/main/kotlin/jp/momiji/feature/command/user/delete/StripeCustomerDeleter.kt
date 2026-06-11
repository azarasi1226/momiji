package jp.momiji.feature.command.user.delete

import jp.momiji.event.user.UserDeletedEvent
import jp.momiji.feature.command.InitialPosition
import jp.momiji.feature.command.pooledStreamingProcessorFor
import jp.momiji.port.payment.PaymentGateway
import org.axonframework.messaging.eventhandling.annotation.EventHandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

/**
 * ユーザー削除イベントを受けて、 Stripe 側の Customer（cus_）を削除する外部副作用ハンドラ。
 *
 * [IdpUserDeleter]（IdP 側のユーザー削除）と同型。 cus_ はイベントに載っているもの（コマンド時点の State 由来）を
 * 使うので、 ReadModel の反映遅延や削除順序に依存しない。 カード未登録ユーザーは [UserDeletedEvent.stripeCustomerId]
 * が null なので何もしない。 削除は冪等（[PaymentGateway.deleteCustomer] が恒久エラーを握り、 一時障害はリトライ）。
 */
@Component
@Profile("payment-stripe")
class StripeCustomerDeleter(
    private val paymentGateway: PaymentGateway,
) {
    @EventHandler
    fun on(event: UserDeletedEvent) {
        // そのユーザが一度もクレジットカードを登録シていないならCustomerIdはnullなので、早期return
        val stripeCustomerId = event.stripeCustomerId ?: return
        paymentGateway.deleteCustomer(stripeCustomerId)
    }

    @Configuration
    @Profile("payment-stripe")
    class Config {
        @Bean
        fun stripeCustomerDeleterProcessor() =
            pooledStreamingProcessorFor<StripeCustomerDeleter>("stripe-customer-delete", InitialPosition.LATEST)
    }
}
