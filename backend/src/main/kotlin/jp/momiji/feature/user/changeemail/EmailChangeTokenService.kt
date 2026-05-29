package jp.momiji.feature.user.changeemail

import com.nimbusds.jose.JOSEException
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.MACSigner
import com.nimbusds.jose.crypto.MACVerifier
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.text.ParseException
import java.time.Duration
import java.time.Instant
import java.util.Date

private val logger = KotlinLogging.logger {}

data class EmailChangePayload(
    val userId: String,
    val newEmail: String,
)

@Component
class EmailChangeTokenService(
    @Value("\${momiji.email-change.secret}") private val secret: String,
    @Value("\${momiji.email-change.expiry:PT1H}") private val expiry: Duration,
) {
    init {
        // HS256は最低256bit(=32byte)のキーが必要。短いと MACSigner が実行時に KeyLengthException を投げるので、
        // アプリ起動時に明確なメッセージで fail-fast させる。
        require(secret.toByteArray().size >= MIN_SECRET_BYTES) {
            "momiji.email-change.secret は HS256 のため最低 $MIN_SECRET_BYTES バイト(256bit)必要です。" +
                "現在の長さ: ${secret.toByteArray().size} バイト"
        }
    }

    private val signer = MACSigner(secret.toByteArray())
    private val verifier = MACVerifier(secret.toByteArray())

    companion object {
        private const val MIN_SECRET_BYTES = 32
    }

    fun sign(payload: EmailChangePayload): String {
        val claims =
            JWTClaimsSet
                .Builder()
                .claim("userId", payload.userId)
                .claim("newEmail", payload.newEmail)
                .expirationTime(Date.from(Instant.now().plus(expiry)))
                .build()

        return SignedJWT(JWSHeader(JWSAlgorithm.HS256), claims)
            .apply { sign(signer) }
            .serialize()
    }

    /**
     * 失敗の意味別ログを残しつつ、 caller には null を返す。
     *
     * - [ParseException]: そもそも JWT 形式じゃない（クライアントが改ざんしたか別物を送ってきた）
     * - [JOSEException]: 署名検証時の暗号エラー（鍵不一致、 アルゴリズム不一致 など）
     * - 署名検証 false: 内容改ざんもしくは別 secret 由来の token
     * - 期限切れ: 正規 token だが時間切れ
     * - claim 欠落: 期待した claim が入ってない（version 不一致 etc.）
     */
    fun verify(token: String): EmailChangePayload? {
        val jwt =
            try {
                SignedJWT.parse(token)
            } catch (e: ParseException) {
                logger.debug(e) { "JWT のパースに失敗（形式不正）" }
                return null
            }

        val signatureValid =
            try {
                jwt.verify(verifier)
            } catch (e: JOSEException) {
                logger.warn(e) { "JWT 署名検証で暗号エラー" }
                return null
            }
        if (!signatureValid) {
            logger.warn { "JWT 署名が一致しない（改ざんもしくは別 secret 由来の token）" }
            return null
        }

        val claims = jwt.jwtClaimsSet
        if (Date().after(claims.expirationTime)) {
            logger.debug { "JWT 期限切れ exp=${claims.expirationTime}" }
            return null
        }

        val userId = claims.getStringClaim("userId")
        val newEmail = claims.getStringClaim("newEmail")
        if (userId == null || newEmail == null) {
            logger.warn { "JWT に期待した claim が入っていない: userId=$userId, newEmail=$newEmail" }
            return null
        }

        return EmailChangePayload(userId = userId, newEmail = newEmail)
    }
}
