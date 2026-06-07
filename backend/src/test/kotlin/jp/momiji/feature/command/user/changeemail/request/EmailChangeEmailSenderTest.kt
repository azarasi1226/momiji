package jp.momiji.feature.command.user.changeemail.request

import io.mockk.slot
import io.mockk.verify
import jp.momiji.MomijiIntegrationTestBase
import jp.momiji.event.user.EmailChangeRequestedEvent
import jp.momiji.eventually
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

/**
 * [jp.momiji.feature.command.user.changeemail.request.EmailChangeEmailSender] (EventHandler) 単体の統合テスト。
 *
 * CommandHandler のテスト ([jp.momiji.feature.user.RequestEmailChangeTest]) は
 * 「コマンドを受けて EmailChangeRequestedEvent を発行する」までの責務。
 * このテストは **EventHandler 側の責務** だけに焦点を絞る:
 * 「EmailChangeRequestedEvent が流れてきたら新メール宛にトークン付き本文を送る」
 *
 * AxonTestFixture の `.when().event(...)` で event を直接 publish して、
 * Mockk spy で MailSender への副作用が起きたことを検証する。
 */
class EmailChangeEmailSenderTest : MomijiIntegrationTestBase() {
    @Test
    fun `EmailChangeRequestedEvent でトークン付き本文が新メール宛に送られる`() {
        val userId = "01HXYZSENDER0000000000001"
        val newEmail = "send-target@example.com"

        val event = EmailChangeRequestedEvent(userId = userId, newEmail = newEmail)

        fixture
            .given()
            .noPriorActivity()
            .`when`()
            .event(event)
            .then()
            .events(event)

        val bodySlot = slot<String>()
        eventually {
            verify(exactly = 1) {
                mailSender.send(newEmail, "メールアドレス変更の確認", capture(bodySlot))
            }
        }
        assertTrue(bodySlot.captured.contains("トークン"), "本文にトークン案内が含まれること")
    }
}
