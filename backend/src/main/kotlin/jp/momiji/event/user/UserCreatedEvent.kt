package jp.momiji.event.user

import jp.momiji.event.MomijiEventTag
import org.axonframework.eventsourcing.annotation.EventTag

data class UserCreatedEvent(
    @EventTag(key = MomijiEventTag.USER_ID)
    val id: String,
    val email: String,
)
