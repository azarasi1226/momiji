package jp.momiji.feature.command.user.changeemail.confirm

import iss.jooq.generated.tables.references.LOOKUP_EMAIL
import jp.momiji.event.MomijiEventTag
import jp.momiji.event.user.EmailChangeConfirmedEvent
import jp.momiji.event.user.UserCreatedEvent
import jp.momiji.event.user.UserDeletedEvent
import jp.momiji.feature.command.CommandResult
import jp.momiji.feature.command.user.changeemail.EmailChangeTokenService
import org.axonframework.eventsourcing.annotation.EventSourcingHandler
import org.axonframework.eventsourcing.annotation.reflection.EntityCreator
import org.axonframework.extension.spring.stereotype.EventSourced
import org.axonframework.messaging.commandhandling.annotation.CommandHandler
import org.axonframework.messaging.eventhandling.gateway.EventAppender
import org.axonframework.modelling.annotation.InjectEntity
import org.jooq.DSLContext
import org.springframework.stereotype.Component

@Component
class ConfirmEmailChangeCommandHandler(
    private val dsl: DSLContext,
    private val emailChangeTokenService: EmailChangeTokenService,
) {
    @CommandHandler
    fun handle(
        command: ConfirmEmailChangeCommand,
        @InjectEntity state: State,
        eventAppender: EventAppender,
    ): CommandResult {
        if (!state.created || state.deleted) {
            return ConfirmEmailChangeCommandResult.userNotFound()
        }

        // 形式チェックは EmailChangeToken 値オブジェクトが gRPC 入口で済ませているので、
        // ここでは平文を取り出して 署名 + 期限 検証する。
        val payload = emailChangeTokenService.verify(command.token.value)
        if (payload == null) {
            return ConfirmEmailChangeCommandResult.invalidToken()
        }

        if (command.userId != payload.userId) {
            return ConfirmEmailChangeCommandResult.userMismatch()
        }

        if (emailAlreadyExists(payload.newEmail)) {
            return ConfirmEmailChangeCommandResult.emailAlreadyInUse()
        }

        eventAppender.append(
            EmailChangeConfirmedEvent(
                userId = payload.userId,
                email = payload.newEmail,
                previousEmail = state.currentEmail,
            ),
        )
        return ConfirmEmailChangeCommandResult.success()
    }

    private fun emailAlreadyExists(email: String): Boolean =
        dsl.fetchCount(
            LOOKUP_EMAIL,
            LOOKUP_EMAIL.EMAIL.eq(email),
        ) > 0

    @EventSourced(tagKey = MomijiEventTag.USER_ID, idType = String::class)
    class State(
        var created: Boolean,
        var deleted: Boolean,
        // 旧メール通知の宛先として、EmailChangeConfirmedEvent.previousEmail に渡すために保持
        var currentEmail: String,
    ) {
        @EntityCreator
        constructor() : this(
            created = false,
            deleted = false,
            currentEmail = "",
        )

        @EventSourcingHandler
        fun evolve(event: UserCreatedEvent) {
            created = true
            currentEmail = event.email
        }

        @EventSourcingHandler
        fun evolve(event: UserDeletedEvent) {
            deleted = true
        }

        @EventSourcingHandler
        fun evolve(event: EmailChangeConfirmedEvent) {
            currentEmail = event.email
        }
    }
}
