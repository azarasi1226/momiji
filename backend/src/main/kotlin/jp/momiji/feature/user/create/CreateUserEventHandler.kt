package jp.momiji.feature.user.create

import iss.jooq.generated.tables.LookupExternalIdentities.Companion.LOOKUP_EXTERNAL_IDENTITIES
import iss.jooq.generated.tables.references.LOOKUP_EMAIL
import jp.momiji.events.user.ExternalIdentityLinkedEvent
import jp.momiji.events.user.UserCreatedEvent
import org.axonframework.extension.spring.config.EventProcessorDefinition
import org.axonframework.messaging.eventhandling.annotation.EventHandler
import org.jooq.DSLContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component

@Component
class CreateUserEventHandler(
  private val dsl: DSLContext
) {
  @EventHandler
  fun on(event: UserCreatedEvent) {
    dsl.insertInto(LOOKUP_EMAIL)
      .set(LOOKUP_EMAIL.EMAIL, event.email)
      .set(LOOKUP_EMAIL.USER_ID, event.id)
      .execute()
  }

  @EventHandler
  fun on(event: ExternalIdentityLinkedEvent) {
    dsl.insertInto(LOOKUP_EXTERNAL_IDENTITIES)
      .set(LOOKUP_EXTERNAL_IDENTITIES.OIDC_ISSUER, event.oidcIssuer)
      .set(LOOKUP_EXTERNAL_IDENTITIES.OIDC_SUBJECT, event.oidcSubject)
      .set(LOOKUP_EXTERNAL_IDENTITIES.IDENTITY_PROVIDER, event.oidcIdentityProvider)
      .set(LOOKUP_EXTERNAL_IDENTITIES.USER_ID, event.userId)
      .execute()
  }

  @Configuration
  class Config {
    @Bean
    fun createUserEventHandlerDefinition() =
      EventProcessorDefinition
        .subscribing(CreateUserEventHandler::class.simpleName!!)
        .assigningHandlers { it.beanType() == CreateUserEventHandler::class.java }
  }
}