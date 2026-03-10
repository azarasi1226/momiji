package jp.momiji.feature.user.changeemail.confirm

import iss.jooq.generated.tables.references.LOOKUP_EXTERNAL_IDENTITIES
import io.github.oshai.kotlinlogging.KotlinLogging
import jp.momiji.events.user.EmailChangeConfirmed
import jp.momiji.feature.idp.IdentityProvider
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
    // IdentityProviderが "LOCAL" のものだけを対象とする。　これには二つの意味がある。
    // 1. 例えば "Google" なものを変更したとしても、IDPにログインするたびにIDP内のemail属性が書き換えられるから意味がない。
    // 2. "LOCAL" はログイン時にemailを使用するので、絶対同期させる必要があるが、 "Google" などの場合はログインに使用しないし、参照もしないため。
    val oidcSubjects = dsl.select(LOOKUP_EXTERNAL_IDENTITIES.OIDC_SUBJECT)
      .from(LOOKUP_EXTERNAL_IDENTITIES)
      .where(LOOKUP_EXTERNAL_IDENTITIES.USER_ID.eq(event.userId))
        .and(LOOKUP_EXTERNAL_IDENTITIES.IDENTITY_PROVIDER.eq(IdentityProvider.LOCAL.name))
      .fetch(LOOKUP_EXTERNAL_IDENTITIES.OIDC_SUBJECT)

    if (oidcSubjects.isEmpty()) {
      logger.error { "対象のSubjectが発見できませんでした。　何かがおかしい...." }
      return
    }

    oidcSubjects.forEach { oidcSubject ->
      // テーブルのカラムはNOT NULL 制約にしてるので "!!" しているよ。
      idpUserClient.updateEmail(oidcSubject!!, event.email)
    }
  }
}
