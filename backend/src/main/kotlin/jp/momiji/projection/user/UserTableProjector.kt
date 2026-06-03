package jp.momiji.projection.user

import iss.jooq.generated.tables.references.USERS
import jp.momiji.event.user.EmailChangeConfirmedEvent
import jp.momiji.event.user.UserCreatedEvent
import jp.momiji.event.user.UserDeletedEvent
import jp.momiji.event.user.UserUpdatedEvent
import org.axonframework.messaging.eventhandling.annotation.EventHandler
import org.axonframework.messaging.eventhandling.annotation.Timestamp
import org.jooq.DSLContext
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

@Component
class UserTableProjector(
    private val dsl: DSLContext,
) {
    @EventHandler
    fun on(
        event: UserCreatedEvent,
        @Timestamp timestamp: Instant,
    ) {
        val at = LocalDateTime.ofInstant(timestamp, ZoneId.systemDefault())
        dsl
            .insertInto(USERS)
            .set(USERS.ID, event.id)
            .set(USERS.EMAIL, event.email)
            .set(USERS.NAME, "")
            .set(USERS.CREATED_AT, at)
            .set(USERS.UPDATED_AT, at)
            // onDuplicateKeyIgnore はユニーク制約のついた列を除外するため、 id の他に email でも除外判定が走ってしまう。
            // しかし、コマンド側で LOOKUP_EMAIL を利用したチェックを走らせているため、 email が被った UserCreatedEvent はそもそも発生しない想定。
            .onDuplicateKeyIgnore()
            .execute()
    }

    @EventHandler
    fun on(
        event: UserUpdatedEvent,
        @Timestamp timestamp: Instant,
    ) {
        dsl
            .update(USERS)
            .set(USERS.NAME, event.name)
            .set(USERS.PHONE_NUMBER, event.phoneNumber)
            .set(USERS.POSTAL_CODE, event.postalCode)
            .set(USERS.ADDRESS1, event.address1)
            .set(USERS.ADDRESS2, event.address2)
            .set(USERS.UPDATED_AT, LocalDateTime.ofInstant(timestamp, ZoneId.systemDefault()))
            .where(USERS.ID.eq(event.id))
            .execute()
    }

    @EventHandler
    fun on(event: UserDeletedEvent) {
        dsl
            .deleteFrom(USERS)
            .where(USERS.ID.eq(event.id))
            .execute()
    }

    @EventHandler
    fun on(
        event: EmailChangeConfirmedEvent,
        @Timestamp timestamp: Instant,
    ) {
        dsl
            .update(USERS)
            .set(USERS.EMAIL, event.email)
            .set(USERS.UPDATED_AT, LocalDateTime.ofInstant(timestamp, ZoneId.systemDefault()))
            .where(USERS.ID.eq(event.userId))
            .execute()
    }
}
