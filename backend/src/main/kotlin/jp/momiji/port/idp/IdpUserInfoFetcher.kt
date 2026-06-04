package jp.momiji.port.idp

import com.github.michaelbull.result.getOrElse
import jp.momiji.domain.idp.IdentityProvider
import jp.momiji.domain.user.Email

data class OidcUserInfo(
    val issuer: String,
    val subject: String,
    val email: Email,
    val emailVerified: Boolean,
    val identityProvider: IdentityProvider,
)

interface IdpUserInfoFetcher {
    fun handle(
        subject: String,
        issuer: String,
    ): OidcUserInfo
}

// IDPから送られてくる email が万が一正しくない場合でもユーザーはどうすることもできないため IllegalStateExceptionとする。
internal fun resolveEmail(rawEmail: String): Email = Email.create(rawEmail).getOrElse { throw IllegalStateException(it.message) }
