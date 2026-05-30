package jp.momiji.event.user

import jp.momiji.event.MomijiEventTag
import org.axonframework.eventsourcing.annotation.EventTag

data class EmailChangeRequestedEvent(
    @EventTag(key = MomijiEventTag.USER_ID)
    val userId: String,
    val newEmail: String,
)
