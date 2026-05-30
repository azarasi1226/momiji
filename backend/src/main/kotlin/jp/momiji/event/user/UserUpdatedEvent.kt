package jp.momiji.event.user

import jp.momiji.event.MomijiEventTag
import org.axonframework.eventsourcing.annotation.EventTag

data class UserUpdatedEvent(
    @EventTag(key = MomijiEventTag.USER_ID)
    val id: String,
    val name: String,
    val phoneNumber: String,
    val postalCode: String,
    val address1: String,
    val address2: String,
)
