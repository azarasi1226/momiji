package jp.momiji.feature.user.update

import jp.momiji.events.user.UserCreatedEvent
import jp.momiji.events.user.UserUpdatedEvent
import jp.momiji.feature.MomijiIntegrationTestBase
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
                    name = "Alice",
                    phoneNumber = "090-0000-0000",
                    postalCode = "100-0000",
                    address1 = "東京都千代田区",
                    address2 = "千代田1-1",
                ),
            ).then()
            .resultMessagePayload(UpdateUserCommandResult.success())
            .events(
                UserUpdatedEvent(
                    id = userId,
                    name = "Alice",
                    phoneNumber = "090-0000-0000",
                    postalCode = "100-0000",
                    address1 = "東京都千代田区",
                    address2 = "千代田1-1",
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
                    name = "Bob",
                    phoneNumber = "090-1111-1111",
                    postalCode = "100-0001",
                    address1 = "東京都中央区",
                    address2 = "銀座1-1",
                ),
            ).then()
            .resultMessagePayload(UpdateUserCommandResult.userNotFound())
            .noEvents()
    }
}
