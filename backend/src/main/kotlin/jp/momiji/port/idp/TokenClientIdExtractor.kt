package jp.momiji.port.idp

import org.springframework.security.oauth2.jwt.Jwt

/**
 * access token から「そのトークンを取得したクライアントの ID」を取り出す port。
 *
 * **どのクレームに入っているか・どう取り出すかは IdP ごとに異なる**（IdP 固有の知識）ため、
 * IdP 別の adapter（`jp.momiji.adapter.idp` の Keycloak / Cognito 実装 …）で実装し、
 * `@Profile` で 1 環境 1 実装が有効になるよう配線する（ADR 0003）。 同じ IdP 軸の port である
 * [IdpUserClient] / [IdpUserInfoFetcher] と同じ並びに置く。
 *
 * 取り出した値は `JwtClientIdValidator` が期待クライアントID と突き合わせ、 他クライアント宛トークンの
 * 流用を防ぐ。 新しい IdP を足すときは本 port の adapter を**必ず実装**すること（無いと jwtDecoder が
 * 起動時に依存解決できず fail-fast する）。
 *
 * 単純な単一クレーム読みに限らず、 `aud` 配列のメンバーシップ判定やバージョン依存クレーム
 * （例: Azure AD の v1 `appid` / v2 `azp`）など、 IdP 固有の抽出ロジックもここに閉じ込められる。
 *
 * NOTE: 引数の [Jwt] は Spring Security の型。 他の port/idp が純ドメイン型なのに対し、 ここは
 * 「検証済み JWT のクレームを読む」 という性質上 framework 型に依存する点だけ留意。
 */
fun interface TokenClientIdExtractor {
    /** 取り出せなければ null（呼び出し側でフェイルクローズ）。 */
    fun extract(jwt: Jwt): String?
}
