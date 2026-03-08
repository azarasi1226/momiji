package jp.momiji.events.user

data class EmailChangeConfirmed(
  val userId: String,
  val email: String,
)