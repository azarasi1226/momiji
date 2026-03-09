package jp.momiji.feature.user.changeemail.confirm

import iss.jooq.generated.tables.references.LOOKUP_EMAIL
import jp.momiji.events.user.EmailChangeConfirmed
import org.axonframework.extension.spring.config.ProcessorDefinition
import org.axonframework.messaging.eventhandling.annotation.EventHandler
import org.jooq.DSLContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component

@Component
class ConfirmEmailChangeEventHandler(
  private val dsl: DSLContext,
) {
  @EventHandler
  fun on(event: EmailChangeConfirmed) {
    dsl.update(LOOKUP_EMAIL)
      .set(LOOKUP_EMAIL.EMAIL, event.email)
      .where(LOOKUP_EMAIL.USER_ID.eq(event.userId))
      .execute()
  }

  @Configuration
  class Config {
    @Bean
    fun confirmEmailChangeEventHandlerDefinition() =
      ProcessorDefinition
        .subscribingProcessor(ConfirmEmailChangeEventHandler::class.simpleName)
        .assigningHandlers { it.beanType() == ConfirmEmailChangeEventHandler::class.java }
        .withDefaultSettings()
  }
}
