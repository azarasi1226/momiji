package jp.momiji.feature.command.payment.changedefaultcard

import jp.momiji.MomijiIntegrationTestBase
import jp.momiji.event.payment.CardRegisteredEvent
import jp.momiji.event.payment.DefaultCardChangedEvent
import jp.momiji.event.user.UserCreatedEvent
import org.junit.jupiter.api.Test

class ChangeDefaultCardCommandHandlerTest : MomijiIntegrationTestBase() {
    @Test
    fun `正常系_別のカードをデフォルトに変更できる`() {
        val userId = "01HXYZPAYDEF0000000000000U1"

        fixture
            .given()
            .events(
                UserCreatedEvent(id = userId, email = "def1@example.com"),
                CardRegisteredEvent(
                    userId = userId,
                    paymentMethodId = "pm_def1_a",
                    brand = "visa",
                    last4 = "4242",
                    expMonth = 1,
                    expYear = 2030,
                    default = true,
                ),
                CardRegisteredEvent(
                    userId = userId,
                    paymentMethodId = "pm_def1_b",
                    brand = "mastercard",
                    last4 = "4444",
                    expMonth = 2,
                    expYear = 2031,
                    default = false,
                ),
            ).`when`()
            .command(ChangeDefaultCardCommand(userId = userId, paymentMethodId = "pm_def1_b"))
            .then()
            .resultMessagePayload(ChangeDefaultCardCommandResult.success())
            .events(DefaultCardChangedEvent(userId = userId, paymentMethodId = "pm_def1_b"))
    }

    @Test
    fun `冪等性_既にデフォルトなら新規イベントを出さず成功`() {
        val userId = "01HXYZPAYDEF0000000000000U2"

        fixture
            .given()
            .events(
                UserCreatedEvent(id = userId, email = "def2@example.com"),
                CardRegisteredEvent(
                    userId = userId,
                    paymentMethodId = "pm_def2",
                    brand = "visa",
                    last4 = "4242",
                    expMonth = 1,
                    expYear = 2030,
                    default = true,
                ),
            ).`when`()
            .command(ChangeDefaultCardCommand(userId = userId, paymentMethodId = "pm_def2"))
            .then()
            .resultMessagePayload(ChangeDefaultCardCommandResult.success())
            .noEvents()
    }

    @Test
    fun `保有していないカードはデフォルトにできない`() {
        val userId = "01HXYZPAYDEF0000000000000U3"

        fixture
            .given()
            .events(UserCreatedEvent(id = userId, email = "def3@example.com"))
            .`when`()
            .command(ChangeDefaultCardCommand(userId = userId, paymentMethodId = "pm_unknown"))
            .then()
            .resultMessagePayload(ChangeDefaultCardCommandResult.cardNotFound())
            .noEvents()
    }

    @Test
    fun `ユーザーが存在しないと失敗`() {
        val userId = "01HXYZPAYDEF0000000000000U4"

        fixture
            .given()
            .noPriorActivity()
            .`when`()
            .command(ChangeDefaultCardCommand(userId = userId, paymentMethodId = "pm_x"))
            .then()
            .resultMessagePayload(ChangeDefaultCardCommandResult.userNotFound())
            .noEvents()
    }
}
