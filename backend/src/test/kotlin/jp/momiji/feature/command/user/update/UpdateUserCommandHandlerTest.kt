package jp.momiji.feature.command.user.update

import com.github.michaelbull.result.get
import jp.momiji.MomijiIntegrationTestBase
import jp.momiji.domain.user.Name
import jp.momiji.event.user.UserCreatedEvent
import jp.momiji.event.user.UserDeletedEvent
import jp.momiji.event.user.UserUpdatedEvent
import org.junit.jupiter.api.Test

class UpdateUserCommandHandlerTest : MomijiIntegrationTestBase() {
    @Test
    fun `正常系_ユーザー更新成功`() {
        val userId = "01HXYZTESTUSER0000000000001"

        fixture
            .given()
            .events(
                UserCreatedEvent(
                    id = userId,
                    email = "alice@example.com",
                ),
            ).`when`()
            .command(
                UpdateUserCommand(
                    id = userId,
                    name = Name.create("Alice").get()!!,
                ),
            ).then()
            .resultMessagePayload(UpdateUserCommandResult.success())
            .events(
                UserUpdatedEvent(
                    id = userId,
                    name = "Alice",
                ),
            )
    }

    @Test
    fun `異常系_ユーザー未作成のまま更新するとuserNotFound`() {
        val userId = "01HXYZTESTUSER0000000000002"

        fixture
            .given()
            .noPriorActivity()
            .`when`()
            .command(
                UpdateUserCommand(
                    id = userId,
                    name = Name.create("Bob").get()!!,
                ),
            ).then()
            .resultMessagePayload(UpdateUserCommandResult.userNotFound())
            .noEvents()
    }

    @Test
    fun `異常系_削除済みユーザーを更新するとuserNotFound`() {
        val userId = "01HXYZTESTUSER0000000000003"

        fixture
            .given()
            .events(
                UserCreatedEvent(id = userId, email = "alice@example.com"),
                UserDeletedEvent(id = userId, oidcSubjects = emptyList(), stripeCustomerId = null),
            ).`when`()
            .command(
                UpdateUserCommand(
                    id = userId,
                    name = Name.create("Alice").get()!!,
                ),
            ).then()
            .resultMessagePayload(UpdateUserCommandResult.userNotFound())
            .noEvents()
    }
}
