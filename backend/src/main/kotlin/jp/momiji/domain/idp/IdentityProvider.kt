package jp.momiji.domain.idp

/**
 * momiji が受け入れる Identity Provider の whitelist。
 *
 * - **LOCAL**: IDP (Keycloak / Cognito) の内蔵 user store で認証されたユーザー (パスワード等)
 * - **GOOGLE**: Google アカウントでログインしたユーザー
 *
 * IDP を追加する場合の更新箇所:
 * 1. この enum に列挙追加 (例: `GITHUB`)
 * 2. [IdentityProviderResolver] の `when` に新 enum 用の分岐追加 + 対応する abstract property 追加
 * 3. [jp.momiji.feature.idp.KeycloakUserClient] / [jp.momiji.feature.idp.CognitoUserClient]
 *    の anonymous object で新 abstract property を override
 *
 * 1 を追加すると 2 の `when` が non-exhaustive になり **domain 層でコンパイルエラー** で気付ける。
 * 2 で abstract property を追加すると **各 Client の anonymous object でも override 漏れがコンパイルエラー** になる。
 * 「enum 側だけ追加 / 実装側 mapping 漏れ」 の片肺状態を 2 段階で防ぐ構造。
 *
 * whitelist 違反 ( 未対応 IDP からのログイン ) は [IdentityProviderResolver.resolve] が `null` を返し、
 * 各 Client が fail-closed で [jp.momiji.domain.BusinessException] を投げて拒否する。
 */
enum class IdentityProvider {
    LOCAL,
    GOOGLE,
}
