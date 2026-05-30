package jp.momiji.feature.user.changeemail.confirm

import com.github.michaelbull.result.get
import jp.momiji.MomijiIntegrationTestBase
import jp.momiji.domain.user.EmailChangeToken
import jp.momiji.event.user.EmailChangeConfirmedEvent
import jp.momiji.event.user.UserCreatedEvent
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
        val token = EmailChangeToken.create(tokenService.sign(EmailChangePayload(userId = userId, newEmail = newEmail))).get()!!

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
        val token = EmailChangeToken.create(tokenService.sign(EmailChangePayload(userId = userId, newEmail = "new@example.com"))).get()!!

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
    fun `異常系_署名不正のトークンならinvalidToken`() {
        // 形式 (header.payload.signature の 3 セグメント) は OK だが署名が偽物
        // → 値オブジェクト層は通り、 CommandHandler の verify で弾かれる経路
        val userId = "01HXYZCONFMAIL0000000000003"
        val tamperedToken = EmailChangeToken.create("eyJhbGciOiJIUzI1NiJ9.eyJ1c2VySWQiOiJ4In0.invalid-signature").get()!!

        fixture
            .given()
            .events(
                UserCreatedEvent(id = userId, email = "alice@example.com"),
            ).`when`()
            .command(
                ConfirmEmailChangeCommand(
                    userId = userId,
                    token = tamperedToken,
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
        val token =
            EmailChangeToken
                .create(tokenService.sign(EmailChangePayload(userId = otherUserId, newEmail = "new@example.com")))
                .get()!!

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
        val token = EmailChangeToken.create(tokenService.sign(EmailChangePayload(userId = userId, newEmail = takenEmail))).get()!!

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
