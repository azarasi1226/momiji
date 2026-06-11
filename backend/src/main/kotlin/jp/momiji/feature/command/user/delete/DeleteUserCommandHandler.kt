package jp.momiji.feature.command.user.delete

import iss.jooq.generated.tables.LookupExternalIdentities.Companion.LOOKUP_EXTERNAL_IDENTITIES
import jp.momiji.event.MomijiEventTag
import jp.momiji.event.payment.StripeCustomerRegisteredEvent
import jp.momiji.event.user.UserCreatedEvent
import jp.momiji.event.user.UserDeletedEvent
import jp.momiji.feature.command.CommandResult
import org.axonframework.eventsourcing.annotation.EventSourcingHandler
import org.axonframework.eventsourcing.annotation.reflection.EntityCreator
import org.axonframework.extension.spring.stereotype.EventSourced
import org.axonframework.messaging.commandhandling.annotation.CommandHandler
import org.axonframework.messaging.eventhandling.gateway.EventAppender
import org.axonframework.modelling.annotation.InjectEntity
import org.jooq.DSLContext
import org.springframework.stereotype.Component

@Component
class DeleteUserCommandHandler(
    private val dsl: DSLContext,
) {
    @CommandHandler
    fun handle(
        command: DeleteUserCommand,
        @InjectEntity state: State,
        eventAppender: EventAppender,
    ): CommandResult {
        if (!state.created) {
            return DeleteUserCommandResult.userNotFound()
        }

        if (state.deleted) {
            return DeleteUserCommandResult.success()
        }

        eventAppender.append(
            UserDeletedEvent(
                id = command.id,
                oidcSubjects = findOidcSubjects(command.id),
                // 後続の StripeCustomerDeleter が Stripe 側の Customer を同期削除するための識別子。
                // lookup でなくイベント由来（State の fold）なので projection の反映遅延に左右されない。
                stripeCustomerId = state.stripeCustomerId,
            ),
        )
        return DeleteUserCommandResult.success()
    }

    private fun findOidcSubjects(userId: String): List<String> =
        dsl
            .select(LOOKUP_EXTERNAL_IDENTITIES.OIDC_SUBJECT)
            .from(LOOKUP_EXTERNAL_IDENTITIES)
            .where(LOOKUP_EXTERNAL_IDENTITIES.USER_ID.eq(userId))
            .fetch(LOOKUP_EXTERNAL_IDENTITIES.OIDC_SUBJECT)
            .filterNotNull()

    @EventSourced(tagKey = MomijiEventTag.USER_ID, idType = String::class)
    class State(
        var created: Boolean,
        var deleted: Boolean,
        // Stripe Customer（cus_）。 削除イベントに載せて Stripe 側の同期削除に使う（payment スライスのイベントを fold）。
        var stripeCustomerId: String?,
    ) {
        @EntityCreator
        constructor() : this(
            created = false,
            deleted = false,
            stripeCustomerId = null,
        )

        @EventSourcingHandler
        fun evolve(event: UserCreatedEvent) {
            created = true
        }

        @EventSourcingHandler
        fun evolve(event: UserDeletedEvent) {
            deleted = true
        }

        @EventSourcingHandler
        fun evolve(event: StripeCustomerRegisteredEvent) {
            stripeCustomerId = event.stripeCustomerId
        }
    }
}
