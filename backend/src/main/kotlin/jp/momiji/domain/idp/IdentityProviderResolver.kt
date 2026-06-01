package jp.momiji.domain.idp

import jp.momiji.domain.BusinessError
import jp.momiji.domain.BusinessException

/**
 * 外部 IDP の provider name を whitelist ([IdentityProvider]) に解決する base。
 *
 * - whitelist と「enum → provider name」 の `when` は domain で集中管理
 * - provider name の具体値 ( IDP ごとに違う ) は実装側で abstract property として宣言
 * - 新 enum 追加時は `when` の non-exhaustive と abstract property 漏れの両方で **コンパイルエラー検知**
 * - whitelist 違反は [resolve] が fail-closed で [BusinessException] を投げる
 */
abstract class IdentityProviderResolver {
    /** Google アカウントが外部 IDP 側で何という provider name で表現されるか ( IDP ごとに違う ) */
    protected abstract val googleProviderName: String

    /** provider name から [IdentityProvider] を解決。 whitelist 違反は fail-closed で [BusinessException]。 */
    fun resolve(externalName: String): IdentityProvider =
        IdentityProvider.entries.firstOrNull { externalNameOf(it) == externalName }
            ?: throw BusinessException(BusinessError("未対応のIDPからのログインです: $externalName"))

    /** `null` 戻りは「provider name を持たない」 = [IdentityProvider.LOCAL] のみ該当。 */
    private fun externalNameOf(idp: IdentityProvider): String? =
        when (idp) {
            IdentityProvider.LOCAL -> null
            IdentityProvider.GOOGLE -> googleProviderName
        }
}
