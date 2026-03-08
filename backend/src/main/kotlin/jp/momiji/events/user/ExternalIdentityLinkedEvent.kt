package jp.momiji.events.user

data class ExternalIdentityLinkedEvent(
  val oidcIssuer: String,
  val oidcSubject: String,
  val userId: String,
)
