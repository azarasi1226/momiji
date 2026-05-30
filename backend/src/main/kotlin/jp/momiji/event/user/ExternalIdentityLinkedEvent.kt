package jp.momiji.event.user

import jp.momiji.event.MomijiEventTag
import org.axonframework.eventsourcing.annotation.EventTag

data class ExternalIdentityLinkedEvent(
    val oidcIssuer: String,
    val oidcSubject: String,
    val oidcIdentityProvider: String,
    @EventTag(key = MomijiEventTag.USER_ID)
    val userId: String,
)
