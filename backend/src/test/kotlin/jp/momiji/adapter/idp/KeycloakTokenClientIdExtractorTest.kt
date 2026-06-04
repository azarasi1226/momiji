package jp.momiji.adapter.idp

import org.junit.jupiter.api.Test
import org.springframework.security.oauth2.jwt.Jwt
import kotlin.test.assertEquals
import kotlin.test.assertNull

class KeycloakTokenClientIdExtractorTest {
    private val extractor = KeycloakTokenClientIdExtractor()

    @Test
    fun `azp からクライアントIDを取り出す`() {
        assertEquals("momiji", extractor.extract(jwt("azp" to "momiji")))
    }

    @Test
    fun `azp が無ければ null（client_id にはフォールバックしない）`() {
        assertNull(extractor.extract(jwt("client_id" to "momiji")))
    }

    private fun jwt(vararg claims: Pair<String, Any>): Jwt {
        val builder = Jwt.withTokenValue("token").header("alg", "none")
        claims.forEach { (key, value) -> builder.claim(key, value) }
        return builder.build()
    }
}
