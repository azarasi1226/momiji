package jp.momiji.feature.command.payment.preparecard

import io.grpc.Context
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jp.momiji.config.grpc.GrpcAuthContext
import jp.momiji.domain.BusinessError
import jp.momiji.domain.BusinessException
import jp.momiji.feature.command.CommandResult
import jp.momiji.feature.command.UserIdResolver
import jp.momiji.grpc.momiji.payment.preparecard.prepareCardRegistrationRequest
import jp.momiji.port.payment.PaymentGateway
import kotlinx.coroutines.runBlocking
import org.axonframework.messaging.commandhandling.gateway.CommandGateway
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import java.util.concurrent.CompletableFuture

class PrepareCardRegistrationGrpcServiceTest {
    private val commandGateway = mockk<CommandGateway>()
    private val userIdResolver = mockk<UserIdResolver>()
    private val stripeCustomerReader = mockk<StripeCustomerReader>()
    private val paymentGateway = mockk<PaymentGateway>()
    private val service =
        PrepareCardRegistrationGrpcService(commandGateway, userIdResolver, stripeCustomerReader, paymentGateway)

    private val mockAccessToken = mockk<Jwt>()
    private val mockJwt = mockk<JwtAuthenticationToken> { every { token } returns mockAccessToken }

    private fun <T> withAuth(block: () -> T): T =
        Context
            .current()
            .withValue(GrpcAuthContext.AUTH_KEY, mockJwt)
            .call(block)

    @Test
    fun `既存のStripeCustomerがあれば再利用し、作成も記録コマンドも行わない`() {
        every { userIdResolver.resolve(mockAccessToken) } returns "user-1"
        every { stripeCustomerReader.findByUserId("user-1") } returns "cus_existing"
        every { paymentGateway.createSetupIntent(stripeCustomerId = "cus_existing", userId = "user-1") } returns "seti_secret_1"

        val response = withAuth { runBlocking { service.prepareCardRegistration(prepareCardRegistrationRequest {}) } }

        assertEquals("seti_secret_1", response.clientSecret)
        verify(exactly = 0) { paymentGateway.createCustomer(any()) }
        verify(exactly = 0) { commandGateway.send(any(), CommandResult::class.java) }
    }

    @Test
    fun `Customer未作成なら lazy 作成して記録コマンドを送り、そのCustomerでSetupIntentを作る`() {
        every { userIdResolver.resolve(mockAccessToken) } returns "user-2"
        every { stripeCustomerReader.findByUserId("user-2") } returns null
        every { paymentGateway.createCustomer("user-2") } returns "cus_new"
        every { commandGateway.send(any(), CommandResult::class.java) } returns
            CompletableFuture.completedFuture(CommandResult.success())
        every { paymentGateway.createSetupIntent(stripeCustomerId = "cus_new", userId = "user-2") } returns "seti_secret_2"

        val response = withAuth { runBlocking { service.prepareCardRegistration(prepareCardRegistrationRequest {}) } }

        assertEquals("seti_secret_2", response.clientSecret)
        verify(exactly = 1) {
            commandGateway.send(
                match<PrepareCardRegistrationCommand> {
                    it.userId == "user-2" && it.stripeCustomerId == "cus_new"
                },
                CommandResult::class.java,
            )
        }
    }

    @Test
    fun `記録コマンドが業務エラーならBusinessExceptionとなりSetupIntentは作られない`() {
        every { userIdResolver.resolve(mockAccessToken) } returns "user-3"
        every { stripeCustomerReader.findByUserId("user-3") } returns null
        every { paymentGateway.createCustomer("user-3") } returns "cus_fail"
        every { commandGateway.send(any(), CommandResult::class.java) } returns
            CompletableFuture.completedFuture(CommandResult.fail(BusinessError("ユーザーが存在しません")))

        assertThrows<BusinessException> {
            withAuth { runBlocking { service.prepareCardRegistration(prepareCardRegistrationRequest {}) } }
        }

        verify(exactly = 0) { paymentGateway.createSetupIntent(any(), any()) }
    }
}
