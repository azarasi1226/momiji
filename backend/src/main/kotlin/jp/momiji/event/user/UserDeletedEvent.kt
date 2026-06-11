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
    // Stripe Customer（cus_）を同期削除するための識別子。
    // nullable なのはドメインの事実: Customer は lazy 作成なので、 カード未登録ユーザーには存在しない。
    val stripeCustomerId: String?,
)
