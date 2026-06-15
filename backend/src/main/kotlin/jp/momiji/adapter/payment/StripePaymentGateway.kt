package jp.momiji.adapter.payment

import com.stripe.StripeClient
import com.stripe.exception.InvalidRequestException
import com.stripe.net.RequestOptions
import com.stripe.param.CustomerCreateParams
import com.stripe.param.PaymentIntentCreateParams
import com.stripe.param.RefundCreateParams
import com.stripe.param.SetupIntentCreateParams
import io.github.oshai.kotlinlogging.KotlinLogging
import jp.momiji.port.payment.CardDetails
import jp.momiji.port.payment.PaymentGateway
import jp.momiji.port.payment.PaymentIntentResult
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

/**
 * Stripe を叩く outbound 操作（[PaymentGateway]）の実装。
 *
 * SetupIntent metadata に `user_id`（[STRIPE_METADATA_USER_ID]）を載せておき、 webhook 側
 * （[StripeWebhookParser]）が誰のカードかを復元する。 生カード番号は扱わず、 Stripe が attach した pm_ と
 * 表示用情報のみを橋渡しする。 webhook の受信・署名検証は別ポート [jp.momiji.port.payment.PaymentWebhookParser] に分離。
 */
@Component
@Profile("payment-stripe")
class StripePaymentGateway(
    private val stripeClient: StripeClient,
) : PaymentGateway {
    override fun createCustomer(userId: String): String {
        val params =
            CustomerCreateParams
                .builder()
                .putMetadata(STRIPE_METADATA_USER_ID, userId)
                .build()
        // userId をキーにした Idempotency-Key。 lazy Customer 作成の並走 / リトライで
        // Customer が二重作成される（孤児が残る）のを防ぐ。 同じキーなら Stripe は同一 Customer を返す。24時間以内の冪等性を担保する
        val options =
            RequestOptions
                .builder()
                .setIdempotencyKey("create-customer-$userId")
                .build()
        val customer = stripeClient.v1().customers().create(params, options)
        logger.info { "Stripe Customer を作成しました: userId=$userId customerId=${customer.id}" }
        return customer.id
    }

    override fun createSetupIntent(
        stripeCustomerId: String,
        userId: String,
    ): String {
        val params =
            SetupIntentCreateParams
                .builder()
                .setCustomer(stripeCustomerId)
                .addPaymentMethodType("card")
                // webhook で誰のカードかを復元するため user_id を載せる。
                .putMetadata(STRIPE_METADATA_USER_ID, userId)
                .build()
        return stripeClient
            .v1()
            .setupIntents()
            .create(params)
            .clientSecret
    }

    override fun createPaymentIntent(
        stripeCustomerId: String,
        paymentMethodId: String,
        amount: Long,
        orderId: String,
    ): PaymentIntentResult {
        val params =
            PaymentIntentCreateParams
                .builder()
                .setAmount(amount)
                .setCurrency("jpy")
                .setCustomer(stripeCustomerId)
                .setPaymentMethod(paymentMethodId)
                // カード限定にする。 これを付けないと automatic_payment_methods が既定で有効になり、
                // redirect 系メソッドが混ざって confirmCardPayment（カード専用）と相性が悪くなる（return_url 要求等）。
                // createSetupIntent と同じ方針。
                .addPaymentMethodType("card")
                // confirm はフロントの Stripe.js で行う（オンセッション＝3DS をブラウザで突破させる）ので、 ここでは確定しない。
                // webhook で注文を相関するため order_id を載せる。
                .putMetadata(STRIPE_METADATA_ORDER_ID, orderId)
                .build()
        // order ごとに固定の Idempotency-Key。 リトライ・並走で PaymentIntent が二重作成されるのを防ぐ（同一 pi_・client_secret を返す）。
        // 決済失敗＝注文失敗（カード切り替えの再準備はしない）なので、 1 注文 1 PaymentIntent でよい。
        val options =
            RequestOptions
                .builder()
                .setIdempotencyKey("order-payment-$orderId")
                .build()
        val paymentIntent = stripeClient.v1().paymentIntents().create(params, options)
        return PaymentIntentResult(
            clientSecret = paymentIntent.clientSecret,
            paymentIntentId = paymentIntent.id,
        )
    }

    override fun refundPayment(paymentIntentId: String) {
        try {
            val params =
                RefundCreateParams
                    .builder()
                    .setPaymentIntent(paymentIntentId)
                    .build()
            stripeClient.v1().refunds().create(params)
            logger.info { "Stripe Refund を作成しました: paymentIntentId=$paymentIntentId" }
        } catch (e: InvalidRequestException) {
            // 恒久エラー（既に返金済み・不在等）は冪等に握る。 detach/delete と同じ線引き。
            logger.warn(e) { "Stripe Refund をスキップ（恒久エラー・冪等扱い）: paymentIntentId=$paymentIntentId" }
        }
        // 一時障害は投げて呼び出し側のリトライに乗せる。
    }

    override fun retrievePaymentMethod(paymentMethodId: String): CardDetails {
        val card =
            stripeClient
                .v1()
                .paymentMethods()
                .retrieve(paymentMethodId)
                .card
        return CardDetails(
            brand = card.brand,
            last4 = card.last4,
            expMonth = card.expMonth.toInt(),
            expYear = card.expYear.toInt(),
        )
    }

    override fun detachPaymentMethod(paymentMethodId: String) {
        try {
            stripeClient.v1().paymentMethods().detach(paymentMethodId)
            logger.info { "Stripe PaymentMethod を切り離しました: paymentMethodId=$paymentMethodId" }
        } catch (e: InvalidRequestException) {
            // 4xx の不正リクエスト系（既に切り離し済み / pm_ 不在 = resource_missing 等）は恒久エラー。
            // リトライしても結果は変わらず、 望む終端状態（attach されていない）は実質達成済みなので冪等に握る。
            // ここで投げると pooledStreaming が同じイベントを無限リトライしてセグメントが詰まる。
            // StripeのAPIページでInvalidRequestExceptionが送られてくるのをこの目で確認した。
            logger.warn(e) { "Stripe PaymentMethod の切り離しをスキップ（恒久エラー・冪等扱い）: paymentMethodId=$paymentMethodId" }
        }
        // 上記以外（ApiConnectionException / RateLimitException / 5xx の ApiException 等）は一時障害なので
        // 握らずに投げる → CardDetacher の pooledStreaming processor が無限リトライして自己回復する
        // （既定の PropagatingErrorHandler。 EventProcessorDefinitions の KDoc 参照）。
    }

    override fun deleteCustomer(stripeCustomerId: String) {
        try {
            stripeClient.v1().customers().delete(stripeCustomerId)
            logger.info { "Stripe Customer を削除しました: customerId=$stripeCustomerId" }
        } catch (e: InvalidRequestException) {
            // detachPaymentMethod と同じ線引き: 4xx（既に削除済み / 不在）は恒久エラーなので冪等に握る。
            // StripeのAPIページでInvalidRequestExceptionが送られてくるのをこの目で確認した。
            logger.warn(e) { "Stripe Customer の削除をスキップ（恒久エラー・冪等扱い）: customerId=$stripeCustomerId" }
        }
        // 一時障害は投げて StripeCustomerDeleter の pooledStreaming processor の無限リトライに乗せる。
    }
}

/**
 * SetupIntent / Customer に載せる metadata のキー。 outbound（書く）側の [StripePaymentGateway] と
 * inbound（読む）側の [StripeWebhookParser] で共有し、 文字列のドリフトを防ぐ。
 */
internal const val STRIPE_METADATA_USER_ID = "user_id"

/** PaymentIntent に載せる order_id metadata のキー。 outbound（[StripePaymentGateway]）と inbound（[StripeWebhookParser]）で共有。 */
internal const val STRIPE_METADATA_ORDER_ID = "order_id"
