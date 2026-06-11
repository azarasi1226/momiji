package jp.momiji.feature.command.payment.preparecard

import jp.momiji.MomijiIntegrationTestBase
import jp.momiji.event.payment.StripeCustomerRegisteredEvent
import jp.momiji.event.user.UserCreatedEvent
import org.junit.jupiter.api.Test

class PrepareCardRegistrationCommandHandlerTest : MomijiIntegrationTestBase() {
    @Test
    fun `正常系_初回はStripeCustomerを記録する`() {
        val userId = "01HXYZPAYPRE0000000000000U1"

        fixture
            .given()
            .events(UserCreatedEvent(id = userId, email = "pre1@example.com"))
            .`when`()
            .command(
                PrepareCardRegistrationCommand(userId = userId, stripeCustomerId = "cus_pre1"),
            ).then()
            .resultMessagePayload(PrepareCardRegistrationCommandResult.success())
            .events(
                StripeCustomerRegisteredEvent(userId = userId, stripeCustomerId = "cus_pre1"),
            )
    }

    @Test
    fun `冪等性_既にCustomer記録済みなら新規イベントを出さず成功`() {
        val userId = "01HXYZPAYPRE0000000000000U2"

        fixture
            .given()
            .events(
                UserCreatedEvent(id = userId, email = "pre2@example.com"),
                StripeCustomerRegisteredEvent(userId = userId, stripeCustomerId = "cus_pre2"),
            ).`when`()
            .command(
                PrepareCardRegistrationCommand(userId = userId, stripeCustomerId = "cus_pre2_new"),
            ).then()
            .resultMessagePayload(PrepareCardRegistrationCommandResult.success())
            .noEvents()
    }

    @Test
    fun `ユーザーが存在しないと失敗`() {
        val userId = "01HXYZPAYPRE0000000000000U3"

        fixture
            .given()
            .noPriorActivity()
            .`when`()
            .command(
                PrepareCardRegistrationCommand(userId = userId, stripeCustomerId = "cus_pre3"),
            ).then()
            .resultMessagePayload(PrepareCardRegistrationCommandResult.userNotFound())
            .noEvents()
    }
}
