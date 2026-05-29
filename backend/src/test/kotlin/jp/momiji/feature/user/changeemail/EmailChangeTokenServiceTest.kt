package jp.momiji.feature.user.changeemail

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Duration
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * [EmailChangeTokenService] の単体テスト。
 *
 * Spring 不要・外部 IO 無しの純粋ロジック (JWT sign / verify) なので、 コンストラクタ直接呼びで完結。
 *
 * verify() の 5 経路 (前回 [EmailChangeTokenService.verify] を分解した経路に対応):
 * - 1. JWT 形式不正 (ParseException)
 * - 2. 署名検証で false (改ざん / 別 secret)
 * - 3. 期限切れ
 * - 4. 正常 (sign で生成したものを verify で復元)
 * - 5. secret 長さ違反 (init の require)
 *
 * claim 欠落 (verify の 6 経路目) は sign() が必ず claim を入れる構造なので、 sign 経路では再現不能。
 * 外部から手作りした JWT を流し込めば cover できるが、 攻撃ケースの想定として優先度低なので省略。
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
}
