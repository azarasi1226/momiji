package jp.momiji.events.user

import jp.momiji.events.MomijiEventTag
import org.axonframework.eventsourcing.annotation.EventTag

data class EmailChangeConfirmed(
  @EventTag(key= MomijiEventTag.USER_ID)
  val userId: String,
  val email: String,
)