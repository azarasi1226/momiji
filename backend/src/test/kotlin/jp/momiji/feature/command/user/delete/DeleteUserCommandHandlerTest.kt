package jp.momiji.feature.command.user.delete

import jp.momiji.MomijiIntegrationTestBase
import jp.momiji.event.user.UserCreatedEvent
import jp.momiji.event.user.UserDeletedEvent
import jp.momiji.feature.command.user.delete.DeleteUserCommand
import jp.momiji.feature.command.user.delete.DeleteUserCommandResult
import org.junit.jupiter.api.Test

class DeleteUserCommandHandlerTest : MomijiIntegrationTestBase() {
    @Test
    fun `正常系_ユーザー削除成功`() {
        val userId = "01HXYZTESTUSER0000000000010"

        fixture
            .given()
            .events(
                UserCreatedEvent(
                    id = userId,
                    email = "carol@example.com",
                ),
            ).`when`()
            .command(
                DeleteUserCommand(id = userId),
            ).then()
            .resultMessagePayload(DeleteUserCommandResult.success())
            .events(
                // OIDCリンクが無いので oidcSubjects は空
                UserDeletedEvent(
                    id = userId,
                    oidcSubjects = emptyList(),
                ),
            )
    }

    @Test
    fun `異常系_ユーザー未作成のまま削除するとuserNotFound`() {
        val userId = "01HXYZTESTUSER0000000000011"

        fixture
            .given()
            .noPriorActivity()
            .`when`()
            .command(
                DeleteUserCommand(id = userId),
            ).then()
            .resultMessagePayload(DeleteUserCommandResult.userNotFound())
            .noEvents()
    }

    @Test
    fun `冪等性_すでに削除済みの場合は成功を返しイベントは出ない`() {
        val userId = "01HXYZTESTUSER0000000000012"

        fixture
            .given()
            .events(
                UserCreatedEvent(
                    id = userId,
                    email = "dave@example.com",
                ),
                UserDeletedEvent(
                    id = userId,
                    oidcSubjects = emptyList(),
                ),
            ).`when`()
            .command(
                DeleteUserCommand(id = userId),
            ).then()
            .resultMessagePayload(DeleteUserCommandResult.success())
            .noEvents()
    }
}
