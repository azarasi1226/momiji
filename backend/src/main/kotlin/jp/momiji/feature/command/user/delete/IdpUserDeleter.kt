package jp.momiji.feature.command.user.delete

import jp.momiji.event.user.UserDeletedEvent
import jp.momiji.feature.command.InitialPosition
import jp.momiji.feature.command.pooledStreamingProcessorFor
import jp.momiji.port.idp.IdpUserClient
import org.axonframework.messaging.eventhandling.annotation.EventHandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component

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
