package jp.momiji.feature.idp

import jp.momiji.domain.idp.IdentityProvider

interface IdpUserClient {
    fun updateEmail(
        oidcSubject: String,
        newEmail: String,
    )

    fun deleteUser(oidcSubject: String)

    /**
     * access token から [IdentityProvider] を解決する。 parse + lookup + whitelist 検証を一連で行い、
     * whitelist 違反は fail-closed で [jp.momiji.domain.BusinessException] を投げる。
     *
     * 「parse だけ」 「lookup だけ」 を分離して公開すると caller が whitelist 検査を skip できてしまうため、
     * 一連の操作を 1 メソッドに集約してある ( fail-closed の invariant を caller に委ねない )。
     */
    fun resolveIdentityProvider(accessToken: String): IdentityProvider
}
