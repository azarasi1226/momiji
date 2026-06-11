package jp.momiji.feature.command.payment.recordcard

import jp.momiji.MomijiIntegrationTestBase
import jp.momiji.event.payment.CardDeletedEvent
import jp.momiji.event.payment.CardRegisteredEvent
import jp.momiji.event.user.UserCreatedEvent
import org.junit.jupiter.api.Test

class RecordCardRegistrationCommandHandlerTest : MomijiIntegrationTestBase() {
    @Test
    fun `正常系_初回カードはデフォルトになる`() {
        val userId = "01HXYZPAYREC0000000000000U1"

        fixture
            .given()
            .events(UserCreatedEvent(id = userId, email = "rec1@example.com"))
            .`when`()
            .command(
                RecordCardRegistrationCommand(
                    userId = userId,
                    paymentMethodId = "pm_rec1",
                    brand = "visa",
                    last4 = "4242",
                    expMonth = 12,
                    expYear = 2030,
                ),
            ).then()
            .resultMessagePayload(RecordCardRegistrationCommandResult.success())
            .events(
                CardRegisteredEvent(
                    userId = userId,
                    paymentMethodId = "pm_rec1",
                    brand = "visa",
                    last4 = "4242",
                    expMonth = 12,
                    expYear = 2030,
                    default = true,
                ),
            )
    }

    @Test
    fun `2枚目のカードはデフォルトにならない`() {
        val userId = "01HXYZPAYREC0000000000000U2"

        fixture
            .given()
            .events(
                UserCreatedEvent(id = userId, email = "rec2@example.com"),
                CardRegisteredEvent(
                    userId = userId,
                    paymentMethodId = "pm_rec2_first",
                    brand = "visa",
                    last4 = "4242",
                    expMonth = 1,
                    expYear = 2030,
                    default = true,
                ),
            ).`when`()
            .command(
                RecordCardRegistrationCommand(
                    userId = userId,
                    paymentMethodId = "pm_rec2_second",
                    brand = "mastercard",
                    last4 = "4444",
                    expMonth = 2,
                    expYear = 2031,
                ),
            ).then()
            .resultMessagePayload(RecordCardRegistrationCommandResult.success())
            .events(
                CardRegisteredEvent(
                    userId = userId,
                    paymentMethodId = "pm_rec2_second",
                    brand = "mastercard",
                    last4 = "4444",
                    expMonth = 2,
                    expYear = 2031,
                    default = false,
                ),
            )
    }

    @Test
    fun `default不在の状態で登録すると新カードがdefaultになる（自己修復）`() {
        val userId = "01HXYZPAYREC0000000000000U5"

        // 旧仕様の履歴等で「カードはあるが default が無い」状態でも、 次の登録で default が復元される
        fixture
            .given()
            .events(
                UserCreatedEvent(id = userId, email = "rec5@example.com"),
                CardRegisteredEvent(
                    userId = userId,
                    paymentMethodId = "pm_rec5_old_default",
                    brand = "visa",
                    last4 = "4242",
                    expMonth = 1,
                    expYear = 2030,
                    default = true,
                ),
                CardRegisteredEvent(
                    userId = userId,
                    paymentMethodId = "pm_rec5_keep",
                    brand = "mastercard",
                    last4 = "4444",
                    expMonth = 2,
                    expYear = 2031,
                    default = false,
                ),
                // default だったカードだけが消えた（昇格イベント無しの旧履歴を想定）
                CardDeletedEvent(userId = userId, paymentMethodId = "pm_rec5_old_default"),
            ).`when`()
            .command(
                RecordCardRegistrationCommand(
                    userId = userId,
                    paymentMethodId = "pm_rec5_new",
                    brand = "jcb",
                    last4 = "0000",
                    expMonth = 3,
                    expYear = 2032,
                ),
            ).then()
            .resultMessagePayload(RecordCardRegistrationCommandResult.success())
            .events(
                CardRegisteredEvent(
                    userId = userId,
                    paymentMethodId = "pm_rec5_new",
                    brand = "jcb",
                    last4 = "0000",
                    expMonth = 3,
                    expYear = 2032,
                    default = true,
                ),
            )
    }

    @Test
    fun `冪等性_同じpmは再登録しても新規イベントを出さず成功`() {
        val userId = "01HXYZPAYREC0000000000000U3"

        fixture
            .given()
            .events(
                UserCreatedEvent(id = userId, email = "rec3@example.com"),
                CardRegisteredEvent(
                    userId = userId,
                    paymentMethodId = "pm_rec3",
                    brand = "visa",
                    last4 = "4242",
                    expMonth = 1,
                    expYear = 2030,
                    default = true,
                ),
            ).`when`()
            .command(
                RecordCardRegistrationCommand(
                    userId = userId,
                    paymentMethodId = "pm_rec3",
                    brand = "visa",
                    last4 = "4242",
                    expMonth = 1,
                    expYear = 2030,
                ),
            ).then()
            .resultMessagePayload(RecordCardRegistrationCommandResult.success())
            .noEvents()
    }

    @Test
    fun `ユーザーが存在しないと失敗`() {
        val userId = "01HXYZPAYREC0000000000000U4"

        fixture
            .given()
            .noPriorActivity()
            .`when`()
            .command(
                RecordCardRegistrationCommand(
                    userId = userId,
                    paymentMethodId = "pm_rec4",
                    brand = "visa",
                    last4 = "4242",
                    expMonth = 1,
                    expYear = 2030,
                ),
            ).then()
            .resultMessagePayload(RecordCardRegistrationCommandResult.userNotFound())
            .noEvents()
    }
}
