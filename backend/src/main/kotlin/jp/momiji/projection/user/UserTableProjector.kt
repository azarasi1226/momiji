package jp.momiji.projection.user

import iss.jooq.generated.tables.references.USERS
import jp.momiji.events.user.EmailChangeConfirmed
import jp.momiji.events.user.UserCreatedEvent
import org.axonframework.messaging.eventhandling.annotation.EventHandler
import org.jooq.DSLContext
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class UserTableProjector(
  private val dsl: DSLContext
) {
  @EventHandler
  fun on(event: UserCreatedEvent) {
    val now = LocalDateTime.now()
    dsl.insertInto(USERS)
      .set(USERS.ID, event.id)
      .set(USERS.EMAIL, event.email)
      .set(USERS.NAME, "")
      .set(USERS.CREATED_AT, now)
      .set(USERS.UPDATED_AT, now)
      .onDuplicateKeyIgnore()
      .execute()
  }

  @EventHandler
  fun on(event: EmailChangeConfirmed) {
    dsl.update(USERS)
      .set(USERS.EMAIL, event.email)
      .set(USERS.UPDATED_AT, LocalDateTime.now())
      .where(USERS.ID.eq(event.userId))
      .execute()
  }
}
