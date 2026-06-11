package jp.momiji.feature.command.payment.deletecard

import jp.momiji.MomijiIntegrationTestBase
import jp.momiji.event.payment.CardDeletedEvent
import jp.momiji.event.payment.CardRegisteredEvent
import jp.momiji.event.payment.DefaultCardChangedEvent
import jp.momiji.event.user.UserCreatedEvent
import org.junit.jupiter.api.Test

class DeleteCardCommandHandlerTest : MomijiIntegrationTestBase() {
    @Test
    fun `正常系_保有カードを削除できる`() {
        val userId = "01HXYZPAYDEL0000000000000U1"

        fixture
            .given()
            .events(
                UserCreatedEvent(id = userId, email = "del1@example.com"),
                CardRegisteredEvent(
                    userId = userId,
                    paymentMethodId = "pm_del1",
                    brand = "visa",
                    last4 = "4242",
                    expMonth = 1,
                    expYear = 2030,
                    default = true,
                ),
            ).`when`()
            .command(DeleteCardCommand(userId = userId, paymentMethodId = "pm_del1"))
            .then()
            .resultMessagePayload(DeleteCardCommandResult.success())
            .events(CardDeletedEvent(userId = userId, paymentMethodId = "pm_del1"))
    }

    @Test
    fun `デフォルトカードを削除すると最古の残カードが昇格する`() {
        val userId = "01HXYZPAYDEL0000000000000U4"

        fixture
            .given()
            .events(
                UserCreatedEvent(id = userId, email = "del4@example.com"),
                CardRegisteredEvent(
                    userId = userId,
                    paymentMethodId = "pm_del4_a",
                    brand = "visa",
                    last4 = "4242",
                    expMonth = 1,
                    expYear = 2030,
                    default = true,
                ),
                CardRegisteredEvent(
                    userId = userId,
                    paymentMethodId = "pm_del4_b",
                    brand = "mastercard",
                    last4 = "4444",
                    expMonth = 2,
                    expYear = 2031,
                    default = false,
                ),
                CardRegisteredEvent(
                    userId = userId,
                    paymentMethodId = "pm_del4_c",
                    brand = "jcb",
                    last4 = "0000",
                    expMonth = 3,
                    expYear = 2032,
                    default = false,
                ),
            ).`when`()
            .command(DeleteCardCommand(userId = userId, paymentMethodId = "pm_del4_a"))
            .then()
            .resultMessagePayload(DeleteCardCommandResult.success())
            .events(
                CardDeletedEvent(userId = userId, paymentMethodId = "pm_del4_a"),
                // 最古の残カード（b）が昇格する
                DefaultCardChangedEvent(userId = userId, paymentMethodId = "pm_del4_b"),
            )
    }

    @Test
    fun `デフォルトでないカードの削除では昇格は起きない`() {
        val userId = "01HXYZPAYDEL0000000000000U5"

        fixture
            .given()
            .events(
                UserCreatedEvent(id = userId, email = "del5@example.com"),
                CardRegisteredEvent(
                    userId = userId,
                    paymentMethodId = "pm_del5_a",
                    brand = "visa",
                    last4 = "4242",
                    expMonth = 1,
                    expYear = 2030,
                    default = true,
                ),
                CardRegisteredEvent(
                    userId = userId,
                    paymentMethodId = "pm_del5_b",
                    brand = "mastercard",
                    last4 = "4444",
                    expMonth = 2,
                    expYear = 2031,
                    default = false,
                ),
            ).`when`()
            .command(DeleteCardCommand(userId = userId, paymentMethodId = "pm_del5_b"))
            .then()
            .resultMessagePayload(DeleteCardCommandResult.success())
            .events(CardDeletedEvent(userId = userId, paymentMethodId = "pm_del5_b"))
    }

    @Test
    fun `冪等性_存在しないカードの削除はno-opで成功`() {
        val userId = "01HXYZPAYDEL0000000000000U2"

        fixture
            .given()
            .events(UserCreatedEvent(id = userId, email = "del2@example.com"))
            .`when`()
            .command(DeleteCardCommand(userId = userId, paymentMethodId = "pm_unknown"))
            .then()
            .resultMessagePayload(DeleteCardCommandResult.success())
            .noEvents()
    }

    @Test
    fun `ユーザーが存在しないと失敗`() {
        val userId = "01HXYZPAYDEL0000000000000U3"

        fixture
            .given()
            .noPriorActivity()
            .`when`()
            .command(DeleteCardCommand(userId = userId, paymentMethodId = "pm_x"))
            .then()
            .resultMessagePayload(DeleteCardCommandResult.userNotFound())
            .noEvents()
    }
}
