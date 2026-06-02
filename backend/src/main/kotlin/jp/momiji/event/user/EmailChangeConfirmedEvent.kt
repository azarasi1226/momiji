package jp.momiji.event.user

import jp.momiji.event.MomijiEventTag
import org.axonframework.eventsourcing.annotation.EventTag
import org.axonframework.messaging.eventhandling.annotation.Event

@Event(namespace = "momiji.user", name = "EmailChangeConfirmedEvent")
data class EmailChangeConfirmedEvent(
    @EventTag(key = MomijiEventTag.USER_ID)
    val userId: String,
    val email: String,
    val previousEmail: String,
)
