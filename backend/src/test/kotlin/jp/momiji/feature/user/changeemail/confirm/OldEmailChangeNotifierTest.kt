package jp.momiji.feature.user.changeemail.confirm

import io.mockk.verify
import jp.momiji.MomijiIntegrationTestBase
import jp.momiji.event.user.EmailChangeConfirmedEvent
import jp.momiji.eventually
import org.junit.jupiter.api.Test

/**
 * [OldEmailChangeNotifier] (EventHandler) 単体の統合テスト。
 *
 * 「[EmailChangeConfirmedEvent] が流れたら、 旧メールアドレス宛に変更完了通知を送る」
 * という EventHandler の責務だけに焦点を絞る。
 *
 * `EmailChangeConfirmedEvent.previousEmail` が乗っているので、 Read DB を引かずに済む。
 */
class OldEmailChangeNotifierTest : MomijiIntegrationTestBase() {
    @Test
    fun `EmailChangeConfirmedEvent で旧メール宛に変更完了通知が送られる`() {
        val userId = "01HXYZOLDNOTIFY00000000001"
        val previousEmail = "old@example.com"
        val newEmail = "new@example.com"
        val event =
            EmailChangeConfirmedEvent(
                userId = userId,
                email = newEmail,
                previousEmail = previousEmail,
            )

        fixture
            .given()
            .noPriorActivity()
            .`when`()
            .event(event)
            .then()
            .events(event)

        eventually {
            verify(exactly = 1) {
                mailSender.send(previousEmail, "メールアドレスが変更されました", any())
            }
        }
    }
}
