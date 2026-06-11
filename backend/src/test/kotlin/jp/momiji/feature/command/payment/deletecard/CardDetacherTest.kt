package jp.momiji.feature.command.payment.deletecard

import io.mockk.verify
import jp.momiji.MomijiIntegrationTestBase
import jp.momiji.event.payment.CardDeletedEvent
import jp.momiji.eventually
import org.junit.jupiter.api.Test

/**
 * [CardDetacher] (EventHandler) 単体の統合テスト。
 *
 * 「[CardDeletedEvent] が流れたら、 その pm_ を Stripe から Detach する」
 * という EventHandler の責務だけに焦点を絞る（[jp.momiji.feature.command.user.delete.IdpUserDeleterTest] と同型）。
 */
class CardDetacherTest : MomijiIntegrationTestBase() {
    @Test
    fun `CardDeletedEvent が流れたら Stripe で Detach される`() {
        val userId = "01HXYZDETACH000000000000U1"
        val event =
            CardDeletedEvent(
                userId = userId,
                paymentMethodId = "pm_detach1",
            )

        fixture
            .given()
            .noPriorActivity()
            .`when`()
            .event(event)
            .then()
            .events(event)

        eventually {
            verify(exactly = 1) { paymentGateway.detachPaymentMethod("pm_detach1") }
        }
    }
}
