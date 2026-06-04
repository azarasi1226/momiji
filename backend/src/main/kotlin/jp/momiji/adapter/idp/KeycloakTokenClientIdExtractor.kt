package jp.momiji.adapter.idp

import jp.momiji.port.idp.TokenClientIdExtractor
import org.springframework.context.annotation.Profile
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.stereotype.Component

/**
 * Keycloak の access token は、 トークンを要求したクライアントID を `azp`（authorized party）に入れる。
 */
@Component
@Profile("idp-keycloak")
class KeycloakTokenClientIdExtractor : TokenClientIdExtractor {
    override fun extract(jwt: Jwt): String? = jwt.getClaimAsString("azp")
}
