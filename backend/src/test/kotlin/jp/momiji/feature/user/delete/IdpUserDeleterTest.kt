package jp.momiji.feature.user.delete

import io.mockk.verify
import jp.momiji.MomijiIntegrationTestBase
import jp.momiji.event.user.UserDeletedEvent
import jp.momiji.eventually
import org.junit.jupiter.api.Test

/**
 * [IdpUserDeleter] (EventHandler) 単体の統合テスト。
 *
 * 「[UserDeletedEvent] が流れたら、 そこに乗っている全 oidcSubject を IDP から削除する」
 * という EventHandler の責務だけに焦点を絞る。
 *
 * `UserDeletedEvent.oidcSubjects` をそのまま使うので、 Read DB 参照なし。
 */
class IdpUserDeleterTest : MomijiIntegrationTestBase() {
    @Test
    fun `UserDeletedEvent に乗った各oidcSubjectがIDPから削除される`() {
        val userId = "01HXYZIDPDEL0000000000001"
        val subject1 = "subj-del-1"
        val subject2 = "subj-del-2"
        val event =
            UserDeletedEvent(
                id = userId,
                oidcSubjects = listOf(subject1, subject2),
            )

        fixture
            .given()
            .noPriorActivity()
            .`when`()
            .event(event)
            .then()
            .events(event)

        eventually {
            verify(exactly = 1) { idpUserClient.deleteUser(subject1) }
            verify(exactly = 1) { idpUserClient.deleteUser(subject2) }
        }
    }

    @Test
    fun `oidcSubjects が空ならIDP削除は呼ばれない`() {
        val userId = "01HXYZIDPDEL0000000000002"
        val event =
            UserDeletedEvent(
                id = userId,
                oidcSubjects = emptyList(),
            )

        fixture
            .given()
            .noPriorActivity()
            .`when`()
            .event(event)
            .then()
            .events(event)

        verify(exactly = 0) { idpUserClient.deleteUser(any()) }
    }
}
