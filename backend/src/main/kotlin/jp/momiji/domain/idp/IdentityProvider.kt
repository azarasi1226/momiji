package jp.momiji.domain.idp

/**
 * OIDC リンク先の IDP 種別を表すドメイン enum。
 *
 * - [LOCAL]: Keycloak / Auth0 / Cognito などの親元 IDP
 * - [GOOGLE]: 親元 IDP と連携している Google ソーシャルログイン
 *
 * 文字列入力からの変換は各 IDP の実装クラス側 ([jp.momiji.feature.idp.KeycloakUserClient],
 * [jp.momiji.feature.idp.CognitoUserClient]) で行う。 ドメイン層は「IDP 種別の表現」 だけを持ち、
 * 各 IDP 固有の文字列フォーマット (Keycloak は `"google"` 小文字、 Cognito は `"Google"` 大文字) を
 * 抱え込まない。
 *
 * Event store には `name` プロパティを使って文字列として保存し、 スキーマ進化リスクを避ける。
 */
enum class IdentityProvider {
    LOCAL,
    GOOGLE,
}
