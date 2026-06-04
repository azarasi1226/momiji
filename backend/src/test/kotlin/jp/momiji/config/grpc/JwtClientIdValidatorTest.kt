package jp.momiji.config.grpc

import org.junit.jupiter.api.Test
import org.springframework.security.oauth2.jwt.Jwt
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * [JwtClientIdValidator] の単体テスト。 抽出は [TokenClientIdExtractor] に委譲しているので、
 * ここでは「抽出値 vs 期待値」の突き合わせ（一致/不一致/抽出不能）だけを stub extractor で検証する。
 */
class JwtClientIdValidatorTest {
    @Test
    fun `抽出値が期待値と一致すれば成功`() {
        val validator = JwtClientIdValidator(extractor = { "momiji" }, expectedClientId = "momiji")
        assertFalse(validator.validate(anyJwt()).hasErrors())
    }

    @Test
    fun `抽出値が期待値と違えば失敗`() {
        val validator = JwtClientIdValidator(extractor = { "other-app" }, expectedClientId = "momiji")
        assertTrue(validator.validate(anyJwt()).hasErrors())
    }

    @Test
    fun `抽出できなければ(null)失敗（フェイルクローズ）`() {
        val validator = JwtClientIdValidator(extractor = { null }, expectedClientId = "momiji")
        assertTrue(validator.validate(anyJwt()).hasErrors())
    }

    private fun anyJwt(): Jwt =
        Jwt
            .withTokenValue("token")
            .header("alg", "none")
            .claim("sub", "user-1")
            .build()
}
