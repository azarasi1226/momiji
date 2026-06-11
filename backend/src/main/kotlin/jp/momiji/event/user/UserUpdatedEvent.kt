package jp.momiji.event.user

import jp.momiji.event.MomijiEventTag
import org.axonframework.eventsourcing.annotation.EventTag
import org.axonframework.messaging.eventhandling.annotation.Event

// プロフィールは email（変更フロー別建て）と name のみ。 住所・電話は配送先イベントが持つ。
@Event(namespace = "momiji.user", name = "UserUpdatedEvent")
data class UserUpdatedEvent(
    @EventTag(key = MomijiEventTag.USER_ID)
    val id: String,
    val name: String,
)
