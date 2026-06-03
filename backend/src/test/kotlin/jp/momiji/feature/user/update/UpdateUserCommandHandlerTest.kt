package jp.momiji.feature.user.update

import com.github.michaelbull.result.get
import jp.momiji.MomijiIntegrationTestBase
import jp.momiji.domain.user.Address1
import jp.momiji.domain.user.Address2
import jp.momiji.domain.user.Name
import jp.momiji.domain.user.PhoneNumber
import jp.momiji.domain.user.PostalCode
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
                    phoneNumber = PhoneNumber.create("090-0000-0000").get()!!,
                    postalCode = PostalCode.create("100-0000").get()!!,
                    address1 = Address1.create("東京都千代田区").get()!!,
                    address2 = Address2.create("千代田1-1").get()!!,
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
                    name = Name.create("Bob").get()!!,
                    phoneNumber = PhoneNumber.create("090-1111-1111").get()!!,
                    postalCode = PostalCode.create("100-0001").get()!!,
                    address1 = Address1.create("東京都中央区").get()!!,
                    address2 = Address2.create("銀座1-1").get()!!,
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
                UserDeletedEvent(id = userId, oidcSubjects = emptyList()),
            ).`when`()
            .command(
                UpdateUserCommand(
                    id = userId,
                    name = Name.create("Alice").get()!!,
                    phoneNumber = PhoneNumber.create("090-0000-0000").get()!!,
                    postalCode = PostalCode.create("100-0000").get()!!,
                    address1 = Address1.create("東京都千代田区").get()!!,
                    address2 = Address2.create("千代田1-1").get()!!,
                ),
            ).then()
            .resultMessagePayload(UpdateUserCommandResult.userNotFound())
            .noEvents()
    }
}
