package jp.momiji.feature.user.changeemail

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.MACSigner
import com.nimbusds.jose.crypto.MACVerifier
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.Instant
import java.util.Date

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

    fun verify(token: String): EmailChangePayload? {
        return try {
            val jwt = SignedJWT.parse(token)
            if (!jwt.verify(verifier)) return null
            if (Date().after(jwt.jwtClaimsSet.expirationTime)) return null
            EmailChangePayload(
                userId = jwt.jwtClaimsSet.getStringClaim("userId"),
                newEmail = jwt.jwtClaimsSet.getStringClaim("newEmail"),
            )
        } catch (_: Exception) {
            null
        }
    }
}
