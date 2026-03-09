package jp.momiji.feature.user.changeemail.confirm

import iss.jooq.generated.tables.references.LOOKUP_EXTERNAL_IDENTITIES
import io.github.oshai.kotlinlogging.KotlinLogging
import jp.momiji.events.user.EmailChangeConfirmed
import jp.momiji.feature.idp.IdpUserClient
import org.axonframework.extension.spring.config.ProcessorDefinition
import org.axonframework.messaging.eventhandling.annotation.EventHandler
import org.jooq.DSLContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

@Component
class EmailToIdpSyncer(
  private val dsl: DSLContext,
  private val idpUserClient: IdpUserClient,
) {
  @EventHandler
  fun on(event: EmailChangeConfirmed) {
    val identities = dsl.select(LOOKUP_EXTERNAL_IDENTITIES.OIDC_SUBJECT)
      .from(LOOKUP_EXTERNAL_IDENTITIES)
      .where(LOOKUP_EXTERNAL_IDENTITIES.USER_ID.eq(event.userId))
      .fetch(LOOKUP_EXTERNAL_IDENTITIES.OIDC_SUBJECT)

    if (identities.isEmpty()) {
      return
    }

    identities.forEach { oidcSubject ->
      idpUserClient.updateEmail(oidcSubject!!, event.email)
    }
  }
}
