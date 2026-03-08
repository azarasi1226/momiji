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
  private val signer = MACSigner(secret.toByteArray())
  private val verifier = MACVerifier(secret.toByteArray())

  fun sign(payload: EmailChangePayload): String {
    val claims = JWTClaimsSet.Builder()
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
