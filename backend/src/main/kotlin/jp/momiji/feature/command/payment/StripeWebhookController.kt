package jp.momiji.feature.command.payment

import com.stripe.exception.SignatureVerificationException
import io.github.oshai.kotlinlogging.KotlinLogging
import jp.momiji.port.payment.PaymentWebhookParser
import jp.momiji.port.payment.StripeWebhookEvent
import kotlinx.coroutines.runBlocking
import org.springframework.context.annotation.Profile
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController

private val logger = KotlinLogging.logger {}

/**
 * Stripe webhook の唯一の受け口（HTTP）。 **全イベント種別をここで受けて各 [StripeWebhookEventHandler] に振り分ける**。
 *
 * Stripe は 1 エンドポイントで全イベントを送ってくるため、 この Controller は特定ユースケースに属さない共有のパッケージに置いています。
 */
@RestController
@Profile("payment-stripe")
class StripeWebhookController(
    private val webhookParser: PaymentWebhookParser,
    private val handlers: List<StripeWebhookEventHandler>,
) {
    @PostMapping("/api/webhooks/stripe")
    fun handle(
        @RequestBody payload: String,
        @RequestHeader("Stripe-Signature") signature: String,
    ): ResponseEntity<Unit> {
        val event =
            try {
                webhookParser.parseWebhookEvent(payload, signature)
            } catch (e: SignatureVerificationException) {
                logger.warn(e) { "Stripe webhook の署名検証に失敗しました" }
                return ResponseEntity.badRequest().build()
            }

        // 関心のないイベントは 200 で無視。 それ以外は登録された全ハンドラに振り分ける（各自が自分の型だけ拾う）。
        if (event !is StripeWebhookEvent.Ignored) {
            // NOTE: ハンドラは suspend（CommandGateway が suspend のため）だが、 このメソッド自体を suspend に
            // すると Spring MVC は Reactor（Mono）を要求して NoClassDefFoundError になる（本プロジェクトは
            // Reactor 非依存。 gRPC の suspend は grpc-kotlin の仕組みで別物）。 webhook は低頻度なので
            // runBlocking でブロッキング橋渡しする。
            runBlocking {
                handlers.forEach { it.handleIfSupported(event) }
            }
        }

        return ResponseEntity.ok().build()
    }
}
