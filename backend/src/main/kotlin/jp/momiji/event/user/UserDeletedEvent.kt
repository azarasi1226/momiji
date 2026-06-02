package jp.momiji.event.user

import jp.momiji.event.MomijiEventTag
import org.axonframework.eventsourcing.annotation.EventTag
import org.axonframework.messaging.eventhandling.annotation.Event

@Event(namespace = "momiji.user", name = "UserDeletedEvent")
data class UserDeletedEvent(
    @EventTag(key = MomijiEventTag.USER_ID)
    val id: String,
    // 　MomijiのユーザーとIDP内のユーザーを同期削除するための識別子
    val oidcSubjects: List<String>,
)
