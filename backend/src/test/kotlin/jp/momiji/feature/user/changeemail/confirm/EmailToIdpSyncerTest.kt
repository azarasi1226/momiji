package jp.momiji.feature.user.changeemail.confirm

import io.mockk.verify
import jp.momiji.domain.idp.IdentityProvider
import jp.momiji.events.user.EmailChangeConfirmedEvent
import jp.momiji.events.user.ExternalIdentityLinkedEvent
import jp.momiji.feature.MomijiIntegrationTestBase
import jp.momiji.feature.eventually
import org.junit.jupiter.api.Test

/**
 * [EmailToIdpSyncer] (EventHandler) 単体の統合テスト。
 *
 * 「[EmailChangeConfirmedEvent] が流れたら、 LOOKUP_EXTERNAL_IDENTITIES から
 *   `LOCAL` の oidcSubject を引いて IDP 側の email も更新する」
 * という EventHandler の責務だけに焦点を絞る。
 *
 * 事前条件として `ExternalIdentityLinkedEvent` を流して LOOKUP テーブルに行を作る。
 * （`CreateUserEventHandler` が subscribing 設定で同期に INSERT する）
 */
class EmailToIdpSyncerTest : MomijiIntegrationTestBase() {
    @Test
    fun `LOCAL IdP リンクがあれば EmailChangeConfirmedEvent で IDP のメールも更新される`() {
        val userId = "01HXYZIDPSYNC00000000000001"
        val oidcSubject = "subj-idpsync-0001"
        val previousEmail = "old@example.com"
        val newEmail = "new@example.com"
        val confirmedEvent =
            EmailChangeConfirmedEvent(
                userId = userId,
                email = newEmail,
                previousEmail = previousEmail,
            )

        fixture
            .given()
            .events(
                ExternalIdentityLinkedEvent(
                    userId = userId,
                    oidcIssuer = "https://idp.example.com",
                    oidcSubject = oidcSubject,
                    oidcIdentityProvider = IdentityProvider.LOCAL.name,
                ),
            ).`when`()
            .event(confirmedEvent)
            .then()
            .events(confirmedEvent)

        eventually {
            verify(exactly = 1) {
                idpUserClient.updateEmail(oidcSubject, newEmail)
            }
        }
    }

    @Test
    fun `GOOGLE IdP リンクしか無ければ IDP 更新は走らない`() {
        val userId = "01HXYZIDPSYNC00000000000002"
        val oidcSubject = "subj-google-0002"
        val newEmail = "new@example.com"
        val confirmedEvent =
            EmailChangeConfirmedEvent(
                userId = userId,
                email = newEmail,
                previousEmail = "old@example.com",
            )

        fixture
            .given()
            .events(
                ExternalIdentityLinkedEvent(
                    userId = userId,
                    oidcIssuer = "https://accounts.google.com",
                    oidcSubject = oidcSubject,
                    oidcIdentityProvider = IdentityProvider.GOOGLE.name,
                ),
            ).`when`()
            .event(confirmedEvent)
            .then()
            .events(confirmedEvent)

        // GOOGLE はログイン都度 email が IDP 側で書き換わるので同期しない設計
        verify(exactly = 0) { idpUserClient.updateEmail(any(), any()) }
    }
}
