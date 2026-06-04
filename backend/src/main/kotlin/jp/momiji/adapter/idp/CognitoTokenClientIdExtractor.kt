package jp.momiji.adapter.idp

import jp.momiji.port.idp.TokenClientIdExtractor
import org.springframework.context.annotation.Profile
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.stereotype.Component

/**
 * Cognito の access token は、 トークンを要求したクライアントID を `client_id` に入れる。
 * （`aud` は持たない / 持つ場合もリソースサーバー識別子なのでクライアント判定には使わない）
 */
@Component
@Profile("idp-cognito")
class CognitoTokenClientIdExtractor : TokenClientIdExtractor {
    override fun extract(jwt: Jwt): String? = jwt.getClaimAsString("client_id")
}
