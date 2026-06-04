package jp.momiji.config.grpc

import jp.momiji.port.idp.TokenClientIdExtractor
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.oauth2.core.OAuth2Error
import org.springframework.security.oauth2.core.OAuth2TokenValidator
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.stereotype.Component

/**
 * JWT が「この momiji クライアント宛に発行されたか」を検証する Validator（audience 相当）。
 *
 * 署名・iss・exp だけでは、 同一 IdP が**別クライアント**へ発行したトークンも通ってしまう。
 * そこで [extractor]（IdP 別 adapter）でトークンからクライアントID を取り出し、 期待値
 * [expectedClientId]（`momiji.oidc.client-id`）と一致するかを検証する（不一致・抽出不能はフェイルクローズ）。
 *
 * 必要な依存（active な IdP の [TokenClientIdExtractor] と期待クライアントID）は本 Bean が自分で受け取り、
 * 利用側 ([GrpcConfig.jwtDecoder]) は完成した Validator を受け取るだけにしている。
 */
@Component
class JwtClientIdValidator(
    private val extractor: TokenClientIdExtractor,
    @Value("\${momiji.oidc.client-id}") private val expectedClientId: String,
) : OAuth2TokenValidator<Jwt> {
    override fun validate(jwt: Jwt): OAuth2TokenValidatorResult {
        val tokenClientId = extractor.extract(jwt)
        return if (tokenClientId == expectedClientId) {
            OAuth2TokenValidatorResult.success()
        } else {
            // トークン側の値はメッセージに出さない（ログ漏洩回避）。 期待値のみ示す。
            OAuth2TokenValidatorResult.failure(
                OAuth2Error(
                    "invalid_token",
                    "トークン内の clientId が想定された値と異なります。 '$expectedClientId'",
                    null,
                ),
            )
        }
    }
}
