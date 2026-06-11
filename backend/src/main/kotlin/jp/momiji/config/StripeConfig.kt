package jp.momiji.config

import com.stripe.StripeClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

/**
 * Stripe クライアントの Bean 配線。
 *
 * idp adapter と同様に `payment-stripe` profile でゲートする。 統合テストはこの profile を有効化せず、
 * [jp.momiji.port.payment.PaymentGateway] を `@MockkBean` で供給するので secret key 不要で動く。
 */
@Configuration
@Profile("payment-stripe")
class StripeConfig {
    @Bean
    fun stripeClient(
        @Value("\${momiji.stripe.secret-key}") secretKey: String,
    ): StripeClient = StripeClient(secretKey)
}
