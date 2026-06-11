package jp.momiji.feature.command.payment.recordcard

import jp.momiji.MomijiIntegrationTestBase
import jp.momiji.event.user.UserCreatedEvent
import jp.momiji.feature.command.payment.CardTestFixtures.defaultChangedEvent
import jp.momiji.feature.command.payment.CardTestFixtures.registeredEvent
import org.junit.jupiter.api.Test

class RecordCardRegistrationCommandHandlerTest : MomijiIntegrationTestBase() {
    private fun command(
        userId: String,
        paymentMethodId: String,
        brand: String = "visa",
        last4: String = "4242",
        expMonth: Int = 1,
        expYear: Int = 2030,
    ) = RecordCardRegistrationCommand(
        userId = userId,
        paymentMethodId = paymentMethodId,
        brand = brand,
        last4 = last4,
        expMonth = expMonth,
        expYear = expYear,
    )

    @Test
    fun `正常系_初回カードはRegisteredとDefaultChangedの2イベントでデフォルトになる`() {
        val userId = "01HXYZPAYREC0000000000000U1"

        fixture
            .given()
            .events(UserCreatedEvent(id = userId, email = "rec1@example.com"))
            .`when`()
            .command(command(userId, "pm_rec1", expMonth = 12))
            .then()
            .resultMessagePayload(RecordCardRegistrationCommandResult.success())
            .events(
                registeredEvent(userId, "pm_rec1", expMonth = 12),
                defaultChangedEvent(userId, "pm_rec1"),
            )
    }

    @Test
    fun `2枚目のカードはRegisteredのみでデフォルトにならない`() {
        val userId = "01HXYZPAYREC0000000000000U2"

        fixture
            .given()
            .events(
                UserCreatedEvent(id = userId, email = "rec2@example.com"),
                registeredEvent(userId, "pm_rec2_first"),
                defaultChangedEvent(userId, "pm_rec2_first"),
            ).`when`()
            .command(command(userId, "pm_rec2_second", brand = "mastercard", last4 = "4444", expMonth = 2, expYear = 2031))
            .then()
            .resultMessagePayload(RecordCardRegistrationCommandResult.success())
            .events(
                registeredEvent(userId, "pm_rec2_second", brand = "mastercard", last4 = "4444", expMonth = 2, expYear = 2031),
            )
    }

    @Test
    fun `default不在の状態で登録すると新カードがdefaultになる（自己修復）`() {
        val userId = "01HXYZPAYREC0000000000000U5"

        // 「カードはあるが default が無い」履歴を生イベントで構築（DefaultChanged 無しの Registered のみ）。
        // 現行ハンドラは作らない状態だが、 旧履歴・将来の経路への防御として自己修復を固定する。
        fixture
            .given()
            .events(
                UserCreatedEvent(id = userId, email = "rec5@example.com"),
                registeredEvent(userId, "pm_rec5_keep", brand = "mastercard", last4 = "4444", expMonth = 2, expYear = 2031),
            ).`when`()
            .command(command(userId, "pm_rec5_new", brand = "jcb", last4 = "0000", expMonth = 3, expYear = 2032))
            .then()
            .resultMessagePayload(RecordCardRegistrationCommandResult.success())
            .events(
                registeredEvent(userId, "pm_rec5_new", brand = "jcb", last4 = "0000", expMonth = 3, expYear = 2032),
                defaultChangedEvent(userId, "pm_rec5_new"),
            )
    }

    @Test
    fun `冪等性_同じpmは再登録しても新規イベントを出さず成功`() {
        val userId = "01HXYZPAYREC0000000000000U3"

        fixture
            .given()
            .events(
                UserCreatedEvent(id = userId, email = "rec3@example.com"),
                registeredEvent(userId, "pm_rec3"),
                defaultChangedEvent(userId, "pm_rec3"),
            ).`when`()
            .command(command(userId, "pm_rec3"))
            .then()
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
            .command(command(userId, "pm_rec4"))
            .then()
            .resultMessagePayload(RecordCardRegistrationCommandResult.userNotFound())
            .noEvents()
    }
}
