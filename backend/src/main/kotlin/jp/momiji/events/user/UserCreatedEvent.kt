package jp.momiji.events.user

data class UserCreatedEvent(
  val id: String,
  val email: String,
)