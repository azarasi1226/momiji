package jp.momiji.feature.user.changeemail

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.MACSigner
import com.nimbusds.jose.crypto.RSASSASigner
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.security.KeyPairGenerator
import java.time.Duration
import java.time.Instant
import java.util.Date
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * [EmailChangeTokenService] の単体テスト。
 *
 * Spring 不要・外部 IO 無しの純粋ロジック (JWT sign / verify) なので、 コンストラクタ直接呼びで完結。
 *
 * verify() の全経路:
 * - 1. JWT 形式不正 (ParseException)
 * - 2. 署名検証で JOSEException (アルゴリズム不一致などの暗号エラー)
 * - 3. 署名検証で false (改ざん / 別 secret)
 * - 4. 期限切れ
 * - 5. claim 欠落 (userId / newEmail が無い)
 * - 6. 正常 (sign で生成したものを verify で復元)
 * - 7. secret 長さ違反 (init の require)
 *
 * 2 と 5 は sign() が常に HS256・両 claim 入りで発行するため sign 経路では再現できず、
 * Nimbus で手作りした JWT を流し込んで cover する。
 */
class EmailChangeTokenServiceTest {
    private val validSecret = "test-secret-must-be-at-least-32-bytes-long"
    private val expiry: Duration = Duration.ofHours(1)
    private val service = EmailChangeTokenService(validSecret, expiry)

    @Test
    fun `sign で発行した token は verify で payload を復元できる`() {
        val payload = EmailChangePayload(userId = "user-1", newEmail = "new@example.com")

        val token = service.sign(payload)
        val verified = service.verify(token)

        assertEquals(payload, verified)
    }

    @Test
    fun `期限切れ token は verify で null (ログレベルは debug)`() {
        // 1ms expiry の service を作り、 sign 後 sleep で確実に期限切れ
        val shortLivedService = EmailChangeTokenService(validSecret, Duration.ofMillis(1))
        val payload = EmailChangePayload(userId = "user-1", newEmail = "new@example.com")

        val token = shortLivedService.sign(payload)
        Thread.sleep(10)

        assertNull(shortLivedService.verify(token))
    }

    @Test
    fun `JWT 形式不正は verify で null (ログレベルは debug)`() {
        assertNull(service.verify("not-a-jwt-at-all"))
    }

    @Test
    fun `空文字も verify で null`() {
        assertNull(service.verify(""))
    }

    @Test
    fun `別 secret で sign された token は verify で null (ログレベルは warn)`() {
        val otherService = EmailChangeTokenService("different-secret-also-32-bytes-or-more", expiry)
        val payload = EmailChangePayload(userId = "user-1", newEmail = "new@example.com")

        val tokenFromOther = otherService.sign(payload)

        // 同じ payload を別 secret で sign したものは、 本 service の verify で reject
        assertNull(service.verify(tokenFromOther))
    }

    @Test
    fun `token を改ざんすると verify で null (ログレベルは warn)`() {
        val payload = EmailChangePayload(userId = "user-1", newEmail = "new@example.com")
        val token = service.sign(payload)

        // payload セグメント (中央) を別の base64url 文字列に置き換える
        val parts = token.split(".")
        val tamperedPayload = "eyJ1c2VySWQiOiJoYWNrZXIifQ" // base64url({"userId":"hacker"})
        val tamperedToken = "${parts[0]}.$tamperedPayload.${parts[2]}"

        assertNull(service.verify(tamperedToken))
    }

    @Test
    fun `secret が 32 byte 未満なら init で IllegalArgumentException`() {
        assertThrows<IllegalArgumentException> {
            EmailChangeTokenService("short-secret", expiry)
        }
    }

    @Test
    fun `署名アルゴリズムが HS256 でない token は verify で null (JOSEException, warn)`() {
        // RSA(RS256) で署名した JWT を HS256 用の MACVerifier で検証すると
        // 「Unsupported JWS algorithm」で JOSEException が飛ぶ。署名 false とは別経路。
        val rsaKeyPair =
            KeyPairGenerator
                .getInstance("RSA")
                .apply { initialize(2048) }
                .generateKeyPair()
        val claims =
            JWTClaimsSet
                .Builder()
                .claim("userId", "user-1")
                .claim("newEmail", "new@example.com")
                .expirationTime(Date.from(Instant.now().plus(expiry)))
                .build()
        val rs256Token =
            SignedJWT(JWSHeader(JWSAlgorithm.RS256), claims)
                .apply { sign(RSASSASigner(rsaKeyPair.private)) }
                .serialize()

        assertNull(service.verify(rs256Token))
    }

    @Test
    fun `userId claim が欠落した token は verify で null (warn)`() {
        assertNull(service.verify(signWithValidSecret(userId = null, newEmail = "new@example.com")))
    }

    @Test
    fun `newEmail claim が欠落した token は verify で null (warn)`() {
        assertNull(service.verify(signWithValidSecret(userId = "user-1", newEmail = null)))
    }

    /**
     * 正しい secret・期限内で署名しつつ、 claim を任意に欠落させた JWT を手作りする。
     * sign() は常に両 claim を入れるため、 claim 欠落経路はこの手作り token でしか再現できない。
     */
    private fun signWithValidSecret(
        userId: String?,
        newEmail: String?,
    ): String {
        val claims =
            JWTClaimsSet
                .Builder()
                .apply {
                    if (userId != null) {
                        claim("userId", userId)
                    }
                    if (newEmail != null) {
                        claim("newEmail", newEmail)
                    }
                }.expirationTime(Date.from(Instant.now().plus(expiry)))
                .build()

        return SignedJWT(JWSHeader(JWSAlgorithm.HS256), claims)
            .apply { sign(MACSigner(validSecret.toByteArray())) }
            .serialize()
    }
}
