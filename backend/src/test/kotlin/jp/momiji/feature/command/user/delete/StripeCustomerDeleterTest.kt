package jp.momiji.feature.command.user.delete

import io.mockk.verify
import jp.momiji.MomijiIntegrationTestBase
import jp.momiji.event.user.UserDeletedEvent
import jp.momiji.eventually
import org.junit.jupiter.api.Test

/**
 * [StripeCustomerDeleter] (EventHandler) 単体の統合テスト。
 *
 * 「[UserDeletedEvent] が流れたら、 そこに乗っている stripeCustomerId を Stripe から削除する」
 * という EventHandler の責務だけに焦点を絞る（[IdpUserDeleterTest] と同型）。
 *
 * `UserDeletedEvent.stripeCustomerId` をそのまま使うので、 Read DB 参照なし。
 */
class StripeCustomerDeleterTest : MomijiIntegrationTestBase() {
    @Test
    fun `UserDeletedEvent に乗った stripeCustomerId が Stripe から削除される`() {
        val userId = "01HXYZSTRDEL0000000000001"
        val event =
            UserDeletedEvent(
                id = userId,
                oidcSubjects = emptyList(),
                stripeCustomerId = "cus_strdel1",
            )

        fixture
            .given()
            .noPriorActivity()
            .`when`()
            .event(event)
            .then()
            .events(event)

        eventually {
            verify(exactly = 1) { paymentGateway.deleteCustomer("cus_strdel1") }
        }
    }

    @Test
    fun `stripeCustomerId が null（カード未登録ユーザー）なら Stripe 削除は呼ばれない`() {
        val userId = "01HXYZSTRDEL0000000000002"
        val event =
            UserDeletedEvent(
                id = userId,
                oidcSubjects = emptyList(),
                stripeCustomerId = null,
            )

        fixture
            .given()
            .noPriorActivity()
            .`when`()
            .event(event)
            .then()
            .events(event)

        verify(exactly = 0) { paymentGateway.deleteCustomer(any()) }
    }
}
