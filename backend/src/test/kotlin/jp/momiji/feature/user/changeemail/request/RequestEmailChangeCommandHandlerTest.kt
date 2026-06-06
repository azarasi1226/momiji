package jp.momiji.feature.user.changeemail.request

import com.github.michaelbull.result.get
import jp.momiji.MomijiIntegrationTestBase
import jp.momiji.domain.user.Email
import jp.momiji.event.user.EmailChangeRequestedEvent
import jp.momiji.event.user.UserCreatedEvent
import jp.momiji.event.user.UserDeletedEvent
import jp.momiji.feature.command.user.changeemail.request.RequestEmailChangeCommand
import jp.momiji.feature.command.user.changeemail.request.RequestEmailChangeCommandResult
import org.junit.jupiter.api.Test

class RequestEmailChangeCommandHandlerTest : MomijiIntegrationTestBase() {
    @Test
    fun `正常系_メール変更リクエスト成功`() {
        val userId = "01HXYZREQMAIL0000000000001"
        val newEmail = "newalice@example.com"

        fixture
            .given()
            .events(
                UserCreatedEvent(id = userId, email = "alice@example.com"),
            ).`when`()
            .command(
                RequestEmailChangeCommand(
                    userId = userId,
                    newEmail = Email.create(newEmail).get()!!,
                ),
            ).then()
            .resultMessagePayload(RequestEmailChangeCommandResult.success())
            .events(
                EmailChangeRequestedEvent(
                    userId = userId,
                    newEmail = newEmail,
                ),
            )
    }

    @Test
    fun `異常系_ユーザー未作成ならuserNotFound`() {
        val userId = "01HXYZREQMAIL0000000000002"

        fixture
            .given()
            .noPriorActivity()
            .`when`()
            .command(
                RequestEmailChangeCommand(
                    userId = userId,
                    newEmail = Email.create("newemail@example.com").get()!!,
                ),
            ).then()
            .resultMessagePayload(RequestEmailChangeCommandResult.userNotFound())
            .noEvents()
    }

    @Test
    fun `異常系_新メールがすでに使用中ならemailAlreadyInUse`() {
        val userId = "01HXYZREQMAIL0000000000003"
        val otherUserId = "01HXYZREQMAIL0000000000004"
        val takenEmail = "taken@example.com"

        fixture
            .given()
            .events(
                UserCreatedEvent(id = userId, email = "alice@example.com"),
                UserCreatedEvent(id = otherUserId, email = takenEmail),
            ).`when`()
            .command(
                RequestEmailChangeCommand(
                    userId = userId,
                    newEmail = Email.create(takenEmail).get()!!, // 別ユーザーが既に使用中
                ),
            ).then()
            .resultMessagePayload(RequestEmailChangeCommandResult.emailAlreadyInUse())
            .noEvents()
    }

    @Test
    fun `異常系_削除済みユーザーならuserNotFound`() {
        val userId = "01HXYZREQMAIL0000000000005"

        fixture
            .given()
            .events(
                UserCreatedEvent(id = userId, email = "alice@example.com"),
                UserDeletedEvent(id = userId, oidcSubjects = emptyList()),
            ).`when`()
            .command(
                RequestEmailChangeCommand(
                    userId = userId,
                    newEmail = Email.create("newemail@example.com").get()!!,
                ),
            ).then()
            .resultMessagePayload(RequestEmailChangeCommandResult.userNotFound())
            .noEvents()
    }
}
