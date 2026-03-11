package jp.momiji.events.user

import jp.momiji.events.MomijiEventTag
import org.axonframework.eventsourcing.annotation.EventTag

data class UserDeletedEvent(
  @EventTag(key= MomijiEventTag.USER_ID)
  val id: String,
  //　MomijiのユーザーとIDP内のユーザーを同期削除するための識別子
  val oidcSubjects: List<String>,
)
