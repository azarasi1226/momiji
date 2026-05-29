package jp.momiji.feature.user.create

import com.github.michaelbull.result.get
import jp.momiji.domain.user.Email
import jp.momiji.events.user.ExternalIdentityLinkedEvent
import jp.momiji.events.user.UserCreatedEvent
import jp.momiji.feature.MomijiIntegrationTestBase
import org.axonframework.test.fixture.AxonTestFixture
import org.junit.jupiter.api.Test

class CreateUserCommandHandlerTest : MomijiIntegrationTestBase() {
    @Test
    fun `正常系_完全新規ユーザー作成は2イベント発行`() {
        // UserCreatedEvent.id / ExternalIdentityLinkedEvent.userId は CommandHandler 内で
        // ulid.nextULID() で動的生成されるため、registerIgnoredField で id 系フィールドを
        // 検証対象から外して、それ以外のフィールドが正しいかを確認する。
        val customFixture =
            AxonTestFixture.with(configurer) { c ->
                c
                    .registerIgnoredField(UserCreatedEvent::class.java, "id")
                    .registerIgnoredField(ExternalIdentityLinkedEvent::class.java, "userId")
            }

        try {
            customFixture
                .given()
                .noPriorActivity()
                .`when`()
                .command(
                    CreateUserCommand(
                        oidcIssuer = "https://idp.example.com",
                        oidcSubject = "subj-brand-new",
                        oidcIdentityProvider = "LOCAL",
                        email = Email.create("brandnew@example.com").get()!!,
                        emailVerified = true,
                    ),
                ).then()
                .resultMessagePayload(CreateUserCommandResult.success())
                .events(
                    // id は ignored
                    UserCreatedEvent(
                        id = "<ignored-by-registerIgnoredField>",
                        email = "brandnew@example.com",
                    ),
                    // userId は ignored
                    ExternalIdentityLinkedEvent(
                        oidcIssuer = "https://idp.example.com",
                        oidcSubject = "subj-brand-new",
                        oidcIdentityProvider = "LOCAL",
                        userId = "<ignored-by-registerIgnoredField>",
                    ),
                )
        } finally {
            customFixture.stop()
        }
    }

    @Test
    fun `異常系_emailVerifiedがfalseならemailNotVerified`() {
        fixture
            .given()
            .noPriorActivity()
            .`when`()
            .command(
                CreateUserCommand(
                    oidcIssuer = "https://idp.example.com",
                    oidcSubject = "subj-not-verified",
                    oidcIdentityProvider = "LOCAL",
                    email = Email.create("notverified@example.com").get()!!,
                    emailVerified = false,
                ),
            ).then()
            .resultMessagePayload(CreateUserCommandResult.emailNotVerified())
            .noEvents()
    }

    @Test
    fun `冪等性_同じissuerSubjectがすでに登録済みなら何もしない`() {
        val existingUserId = "01HXYZCREATETEST0000000001"

        fixture
            .given()
            .events(
                UserCreatedEvent(id = existingUserId, email = "idempotent@example.com"),
                ExternalIdentityLinkedEvent(
                    userId = existingUserId,
                    oidcIssuer = "https://idp.example.com",
                    oidcSubject = "subj-idempotent",
                    oidcIdentityProvider = "LOCAL",
                ),
            ).`when`()
            .command(
                CreateUserCommand(
                    oidcIssuer = "https://idp.example.com",
                    oidcSubject = "subj-idempotent", // 同一subject
                    oidcIdentityProvider = "LOCAL",
                    email = Email.create("idempotent@example.com").get()!!,
                    emailVerified = true,
                ),
            ).then()
            .resultMessagePayload(CreateUserCommandResult.success())
            .noEvents()
    }

    @Test
    fun `既存emailと同じならExternalIdentityLinkedEventのみ出る`() {
        val existingUserId = "01HXYZCREATETEST0000000002"

        fixture
            .given()
            .events(
                UserCreatedEvent(id = existingUserId, email = "link@example.com"),
                ExternalIdentityLinkedEvent(
                    userId = existingUserId,
                    oidcIssuer = "https://idp.example.com",
                    oidcSubject = "subj-existing",
                    oidcIdentityProvider = "LOCAL",
                ),
            ).`when`()
            .command(
                CreateUserCommand(
                    oidcIssuer = "https://accounts.google.com",
                    oidcSubject = "subj-google-link",
                    oidcIdentityProvider = "GOOGLE",
                    email = Email.create("link@example.com").get()!!, // 既存ユーザーと同じemail
                    emailVerified = true,
                ),
            ).then()
            .resultMessagePayload(CreateUserCommandResult.success())
            .events(
                ExternalIdentityLinkedEvent(
                    userId = existingUserId,
                    oidcIssuer = "https://accounts.google.com",
                    oidcSubject = "subj-google-link",
                    oidcIdentityProvider = "GOOGLE",
                ),
            )
    }
}
