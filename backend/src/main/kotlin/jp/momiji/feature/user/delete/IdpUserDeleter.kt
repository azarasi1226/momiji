package jp.momiji.feature.user.delete

import io.github.oshai.kotlinlogging.KotlinLogging
import jp.momiji.event.user.UserDeletedEvent
import jp.momiji.feature.InitialPosition
import jp.momiji.feature.idp.IdpUserClient
import jp.momiji.feature.pooledStreamingProcessorFor
import org.axonframework.messaging.eventhandling.annotation.EventHandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
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
        }
    }

    @Configuration
    class Config {
        @Bean
        fun idpUserDeleterProcessor() = pooledStreamingProcessorFor<IdpUserDeleter>("idp-user-delete", InitialPosition.LATEST)
    }
}
