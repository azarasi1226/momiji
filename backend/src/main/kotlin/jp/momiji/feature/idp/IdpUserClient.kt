package jp.momiji.feature.idp

import jp.momiji.domain.idp.IdentityProvider

interface IdpUserClient {
    fun updateEmail(
        oidcSubject: String,
        newEmail: String,
    )

    fun deleteUser(oidcSubject: String)

    fun getIdentityProvider(accessToken: String): IdentityProvider
}
