package jp.momiji.feature.command.order.preparepayment

import io.grpc.Context
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import jp.momiji.config.grpc.GrpcAuthContext
import jp.momiji.domain.BusinessError
import jp.momiji.domain.BusinessException
import jp.momiji.feature.command.CommandResult
import jp.momiji.feature.command.UserIdResolver
import jp.momiji.grpc.momiji.order.preparepayment.preparePaymentRequest
import jp.momiji.port.payment.PaymentGateway
import jp.momiji.port.payment.PaymentIntentResult
import kotlinx.coroutines.runBlocking
import org.axonframework.messaging.commandhandling.gateway.CommandGateway
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import java.util.concurrent.CompletableFuture

class PreparePaymentGrpcServiceTest {
    private val commandGateway = mockk<CommandGateway>()
    private val userIdResolver = mockk<UserIdResolver>()
    private val paymentGateway = mockk<PaymentGateway>()
    private val payableOrderReader = mockk<PayableOrderReader>()
    private val service = PreparePaymentGrpcService(commandGateway, userIdResolver, paymentGateway, payableOrderReader)

    private val mockAccessToken = mockk<Jwt>()
    private val mockJwt = mockk<JwtAuthenticationToken> { every { token } returns mockAccessToken }

    private fun <T> withAuth(block: () -> T): T =
        Context
            .current()
            .withValue(GrpcAuthContext.AUTH_KEY, mockJwt)
            .call(block)

    private fun request(
        orderId: String,
        paymentMethodId: String,
    ) = preparePaymentRequest {
        this.orderId = orderId
        this.paymentMethodId = paymentMethodId
    }

    @Test
    fun `所有権検証を通れば PaymentIntent を作り PAYMENT_PENDING を記録して client_secret を返す`() {
        every { userIdResolver.resolve(mockAccessToken) } returns "user-1"
        every { payableOrderReader.loadForPayment(orderId = "order-1", userId = "user-1", paymentMethodId = "pm_1") } returns
            PayableOrder(stripeCustomerId = "cus_1", totalAmount = 3000)
        every {
            paymentGateway.createPaymentIntent(stripeCustomerId = "cus_1", paymentMethodId = "pm_1", amount = 3000, orderId = "order-1")
        } returns PaymentIntentResult(clientSecret = "pi_secret_1", paymentIntentId = "pi_1")
        val cmdSlot = slot<PreparePaymentCommand>()
        every { commandGateway.send(capture(cmdSlot), CommandResult::class.java) } returns
            CompletableFuture.completedFuture(CommandResult.success())

        val response = withAuth { runBlocking { service.preparePayment(request("order-1", "pm_1")) } }

        // client_secret をフロントへ返す（Stripe.js の confirm に使う）。
        assertEquals("pi_secret_1", response.clientSecret)
        // 記録コマンドには、 作成した PaymentIntent の pi_ が乗る。
        val sent = cmdSlot.captured
        assertEquals("order-1", sent.orderId)
        assertEquals("pm_1", sent.paymentMethodId)
        assertEquals("pi_1", sent.paymentIntentId)
    }

    @Test
    fun `所有権検証で弾かれたら PaymentIntent を作らずコマンドも送らない`() {
        every { userIdResolver.resolve(mockAccessToken) } returns "user-2"
        every { payableOrderReader.loadForPayment(orderId = "order-2", userId = "user-2", paymentMethodId = "pm_2") } throws
            BusinessException(BusinessError("この注文は決済できません"))

        assertThrows<BusinessException> {
            withAuth { runBlocking { service.preparePayment(request("order-2", "pm_2")) } }
        }
        verify(exactly = 0) { paymentGateway.createPaymentIntent(any(), any(), any(), any()) }
        verify(exactly = 0) { commandGateway.send(any(), CommandResult::class.java) }
    }

    @Test
    fun `記録コマンドが業務エラーなら BusinessException（PaymentIntent は冪等に作成済み）`() {
        every { userIdResolver.resolve(mockAccessToken) } returns "user-3"
        every { payableOrderReader.loadForPayment(orderId = "order-3", userId = "user-3", paymentMethodId = "pm_3") } returns
            PayableOrder(stripeCustomerId = "cus_3", totalAmount = 1500)
        every {
            paymentGateway.createPaymentIntent(stripeCustomerId = "cus_3", paymentMethodId = "pm_3", amount = 1500, orderId = "order-3")
        } returns PaymentIntentResult(clientSecret = "pi_secret_3", paymentIntentId = "pi_3")
        every { commandGateway.send(any(), CommandResult::class.java) } returns
            CompletableFuture.completedFuture(CommandResult.fail(BusinessError("注文が見つかりません")))

        assertThrows<BusinessException> {
            withAuth { runBlocking { service.preparePayment(request("order-3", "pm_3")) } }
        }
        // PaymentIntent は冪等キー付きで先に作られている（業務エラーでも作成自体は行われる）。
        verify(exactly = 1) {
            paymentGateway.createPaymentIntent(stripeCustomerId = "cus_3", paymentMethodId = "pm_3", amount = 1500, orderId = "order-3")
        }
    }
}
