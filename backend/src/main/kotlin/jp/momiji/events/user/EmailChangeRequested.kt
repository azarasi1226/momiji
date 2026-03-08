package jp.momiji.events.user

data class EmailChangeRequested(
  val userId: String,
  val newEmail: String,
)