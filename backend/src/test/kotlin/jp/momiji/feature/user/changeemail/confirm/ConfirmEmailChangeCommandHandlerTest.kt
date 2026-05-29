package jp.momiji.feature.user.changeemail.confirm

import jp.momiji.events.user.EmailChangeConfirmedEvent
import jp.momiji.events.user.UserCreatedEvent
import jp.momiji.feature.MomijiIntegrationTestBase
import jp.momiji.feature.user.changeemail.EmailChangePayload
import jp.momiji.feature.user.changeemail.EmailChangeTokenService
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class ConfirmEmailChangeCommandHandlerTest : MomijiIntegrationTestBase() {
    @Autowired
    lateinit var tokenService: EmailChangeTokenService

    @Test
    fun `正常系_メール変更確認成功でpreviousEmail付きEvent発行`() {
        val userId = "01HXYZCONFMAIL0000000000001"
        val previousEmail = "alice@example.com"
        val newEmail = "newalice@example.com"
        val token = tokenService.sign(EmailChangePayload(userId = userId, newEmail = newEmail))

        fixture
            .given()
            .events(
                UserCreatedEvent(id = userId, email = previousEmail),
            ).`when`()
            .command(
                ConfirmEmailChangeCommand(
                    userId = userId,
                    token = token,
                ),
            ).then()
            .resultMessagePayload(ConfirmEmailChangeCommandResult.success())
            .events(
                EmailChangeConfirmedEvent(
                    userId = userId,
                    email = newEmail,
                    previousEmail = previousEmail,
                ),
            )
    }

    @Test
    fun `異常系_ユーザー未作成ならuserNotFound`() {
        val userId = "01HXYZCONFMAIL0000000000002"
        val token = tokenService.sign(EmailChangePayload(userId = userId, newEmail = "new@example.com"))

        fixture
            .given()
            .noPriorActivity()
            .`when`()
            .command(
                ConfirmEmailChangeCommand(
                    userId = userId,
                    token = token,
                ),
            ).then()
            .resultMessagePayload(ConfirmEmailChangeCommandResult.userNotFound())
            .noEvents()
    }

    @Test
    fun `異常系_無効なトークンならinvalidToken`() {
        val userId = "01HXYZCONFMAIL0000000000003"

        fixture
            .given()
            .events(
                UserCreatedEvent(id = userId, email = "alice@example.com"),
            ).`when`()
            .command(
                ConfirmEmailChangeCommand(
                    userId = userId,
                    token = "this-is-not-a-valid-jwt",
                ),
            ).then()
            .resultMessagePayload(ConfirmEmailChangeCommandResult.invalidToken())
            .noEvents()
    }

    @Test
    fun `異常系_別ユーザー宛のトークンならuserMismatch`() {
        val userId = "01HXYZCONFMAIL0000000000004"
        val otherUserId = "01HXYZCONFMAIL0000000000005"
        // 別ユーザー(otherUserId)宛に発行したtokenを userId が使おうとする
        val token = tokenService.sign(EmailChangePayload(userId = otherUserId, newEmail = "new@example.com"))

        fixture
            .given()
            .events(
                UserCreatedEvent(id = userId, email = "alice@example.com"),
            ).`when`()
            .command(
                ConfirmEmailChangeCommand(
                    userId = userId,
                    token = token,
                ),
            ).then()
            .resultMessagePayload(ConfirmEmailChangeCommandResult.userMismatch())
            .noEvents()
    }

    @Test
    fun `異常系_新メールがすでに使用中ならemailAlreadyInUse`() {
        val userId = "01HXYZCONFMAIL0000000000006"
        val otherUserId = "01HXYZCONFMAIL0000000000007"
        val takenEmail = "taken@example.com"
        val token = tokenService.sign(EmailChangePayload(userId = userId, newEmail = takenEmail))

        fixture
            .given()
            .events(
                UserCreatedEvent(id = userId, email = "alice@example.com"),
                UserCreatedEvent(id = otherUserId, email = takenEmail),
            ).`when`()
            .command(
                ConfirmEmailChangeCommand(
                    userId = userId,
                    token = token,
                ),
            ).then()
            .resultMessagePayload(ConfirmEmailChangeCommandResult.emailAlreadyInUse())
            .noEvents()
    }
}
