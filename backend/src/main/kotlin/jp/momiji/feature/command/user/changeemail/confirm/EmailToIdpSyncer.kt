package jp.momiji.feature.command.user.changeemail.confirm

import iss.jooq.generated.tables.references.LOOKUP_EXTERNAL_IDENTITIES
import jp.momiji.domain.idp.IdentityProvider
import jp.momiji.event.user.EmailChangeConfirmedEvent
import jp.momiji.feature.command.InitialPosition
import jp.momiji.feature.command.pooledStreamingProcessorFor
import jp.momiji.port.idp.IdpUserClient
import org.axonframework.messaging.eventhandling.annotation.EventHandler
import org.jooq.DSLContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component

@Component
class EmailToIdpSyncer(
    private val dsl: DSLContext,
    private val idpUserClient: IdpUserClient,
) {
    @EventHandler
    fun on(event: EmailChangeConfirmedEvent) {
        val oidcSubjects =
            dsl
                .select(LOOKUP_EXTERNAL_IDENTITIES.OIDC_SUBJECT)
                .from(LOOKUP_EXTERNAL_IDENTITIES)
                .where(LOOKUP_EXTERNAL_IDENTITIES.USER_ID.eq(event.userId))
                // IdentityProviderが "LOCAL" のものだけを対象とする。　これには二つの意味がある。
                // 1. 例えば "Google" なものを変更したとしても、ソーシャルIDPにログインするたびにIDP内のemail属性が書き換えられるから結局意味がない。
                // 2. "LOCAL" はログイン時にemailを使用するので、絶対同期させる必要があるが、 "Google" などの場合はログインに使用しないし、参照もしないため。ただの保続情報として属性を持っているだけの模様
                .and(LOOKUP_EXTERNAL_IDENTITIES.IDENTITY_PROVIDER.eq(IdentityProvider.LOCAL.name))
                .fetch(LOOKUP_EXTERNAL_IDENTITIES.OIDC_SUBJECT)
                .filterNotNull()

        oidcSubjects.forEach { oidcSubject ->
            idpUserClient.updateEmail(oidcSubject, event.email)
        }
    }

    @Configuration
    class Config {
        @Bean
        fun emailToIdpSyncerProcessor() = pooledStreamingProcessorFor<EmailToIdpSyncer>("email-to-idp-sync", InitialPosition.LATEST)
    }
}
