package jp.momiji.feature.user.delete

import io.github.oshai.kotlinlogging.KotlinLogging
import jp.momiji.events.user.UserDeletedEvent
import jp.momiji.feature.idp.IdpUserClient
import org.axonframework.messaging.eventhandling.annotation.EventHandler
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

@Component
class IdpUserDeleter(
  private val idpUserClient: IdpUserClient,
) {
  @EventHandler
  fun on(event: UserDeletedEvent) {
    event.oidcSubjects.forEach { oidcSubject ->
      idpUserClient.deleteUser(oidcSubject)
      logger.info { "IDPユーザーを削除しました: oidcSubject=$oidcSubject" }
    }
  }
}
