package jp.momiji.adapter.idp

import org.junit.jupiter.api.Test
import org.springframework.security.oauth2.jwt.Jwt
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CognitoTokenClientIdExtractorTest {
    private val extractor = CognitoTokenClientIdExtractor()

    @Test
    fun `client_id からクライアントIDを取り出す`() {
        assertEquals("momiji", extractor.extract(jwt("client_id" to "momiji")))
    }

    @Test
    fun `client_id が無ければ null（azp にはフォールバックしない）`() {
        assertNull(extractor.extract(jwt("azp" to "momiji")))
    }

    private fun jwt(vararg claims: Pair<String, Any>): Jwt {
        val builder = Jwt.withTokenValue("token").header("alg", "none")
        claims.forEach { (key, value) -> builder.claim(key, value) }
        return builder.build()
    }
}
