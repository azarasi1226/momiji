package jp.momiji.feature.user.delete

import iss.jooq.generated.tables.LookupExternalIdentities.Companion.LOOKUP_EXTERNAL_IDENTITIES
import iss.jooq.generated.tables.references.LOOKUP_EMAIL
import jp.momiji.events.user.UserDeletedEvent
import org.axonframework.extension.spring.config.ProcessorDefinition
import org.axonframework.messaging.eventhandling.annotation.EventHandler
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component

@Component
class DeleteUserEventHandler(
  private val dsl: DSLContext
) {
  @EventHandler
  fun on(event: UserDeletedEvent) {
    dsl.transaction { config ->
      val tx = DSL.using(config)
      tx.deleteFrom(LOOKUP_EMAIL)
        .where(LOOKUP_EMAIL.USER_ID.eq(event.id))
        .execute()

      tx.deleteFrom(LOOKUP_EXTERNAL_IDENTITIES)
        .where(LOOKUP_EXTERNAL_IDENTITIES.USER_ID.eq(event.id))
        .execute()
    }
  }

  @Configuration
  class Config {
    @Bean
    fun deleteUserEventHandlerDefinition() =
      ProcessorDefinition
        .subscribingProcessor(DeleteUserEventHandler::class.simpleName)
        .assigningHandlers { it.beanType() == DeleteUserEventHandler::class.java }
  }
}
