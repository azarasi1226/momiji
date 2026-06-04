package jp.momiji.config.grpc

import io.github.oshai.kotlinlogging.KotlinLogging
import io.grpc.ServerInterceptor
import io.grpc.Status
import jp.momiji.domain.BusinessException
import jp.momiji.domain.ValidationException
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.grpc.server.GlobalServerInterceptor
import org.springframework.grpc.server.exception.GrpcExceptionHandler
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtValidators
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import java.util.UUID

private val logger = KotlinLogging.logger {}

/**
 * gRPCサーバーの横断設定。
 *
 * - 認証Interceptor（全gRPCコールにJWT検証をかける）
 * - 例外ハンドラ（業務例外 → gRPC Status への変換）
 *
 * を Bean として提供する。`spring-grpc-spring-boot-starter` が起動時にこれらを拾い上げ、
 * 自動構成されるgRPCサーバに組み込む。
 *
 * 例外 → ErrorDetail の変換 と StatusException 組み立て は [ErrorDetailMapping] (拡張関数群) に切り出し済み。
 */
@Configuration
class GrpcConfig {
    /**
     * IDP の OIDC discovery から公開鍵を取得して JWT 署名を検証する Decoder。
     *
     * 検証内容: 署名 + iss + exp/nbf（既定）に加えて、 **[JwtClientIdValidator] で「この momiji
     * クライアント宛に発行されたトークンか」**も検証する（これが無いと、 同一 IdP が別クライアントへ
     * 発行したトークンも通ってしまう）。 Validator は自分で IdP 別 [jp.momiji.port.idp.TokenClientIdExtractor]
     * と期待クライアントID を DI するので、 ここは完成した Validator を受け取って合成するだけ。
     *
     * このプロジェクトでは JwtDecoder は gRPC 認証 ([GrpcAuthInterceptor]) でしか使わないので
     * Security 系の独立 Config を分けずにこちらに置く。
     */
    @Bean
    fun jwtDecoder(
        @Value("\${momiji.oidc.issuer-uri}") issuerUri: String,
        clientIdValidator: JwtClientIdValidator,
    ): JwtDecoder {
        val decoder = NimbusJwtDecoder.withIssuerLocation(issuerUri).build()
        // setJwtValidator は既定 validator (timestamp + issuer) を「消してしまう」。
        // そのため、JwtValidators.createDefaultWithIssuer で規定の検証を行いつつ、追加で client 宛検証もする形で DelegatingOAuth2TokenValidator を作る。
        decoder.setJwtValidator(
            DelegatingOAuth2TokenValidator(
                JwtValidators.createDefaultWithIssuer(issuerUri),
                clientIdValidator,
            ),
        )
        return decoder
    }

    /**
     * 全 gRPC コールに対して JWT 認証をかけるグローバルInterceptor。
     *
     * - `@GlobalServerInterceptor`: spring-grpc側で、全gRPCサービスに自動でフックされる印
     * - `@Order(0)`: Interceptorチェーンの一番手前で動作させる（他のInterceptorより先にJWT検証）
     * - [PublicEndpointRegistry] を介して、`@PublicEndpoint` 付きメソッドは認証スキップする
     */
    @Bean
    @Order(0)
    @GlobalServerInterceptor
    fun grpcAuthInterceptor(
        jwtDecoder: JwtDecoder,
        publicEndpointRegistry: PublicEndpointRegistry,
    ): ServerInterceptor = GrpcAuthInterceptor(jwtDecoder, publicEndpointRegistry)

    /**
     * ハンドラ内で投げた業務例外を、 gRPC Status + 構造化 details にマッピングするハンドラ。
     *
     * - [ValidationException] → `INVALID_ARGUMENT` + ValidationError (field 別エラーリスト)
     * - [BusinessException]    → `INVALID_ARGUMENT` + BusinessError (message 1つ)
     * - その他                → `UNKNOWN` + UnknownError (固定メッセージ + correlationId、 詳細はサーバーログ)
     *
     * 詳細は ADR 0002 と [ErrorDetailMapping] 参照。
     */
    @Bean
    fun grpcExceptionHandler(): GrpcExceptionHandler =
        GrpcExceptionHandler { ex ->
            when (ex) {
                is ValidationException ->
                    buildStatusException(
                        Status.INVALID_ARGUMENT,
                        "ValidationError",
                        ex.toErrorDetail(),
                    )
                is BusinessException ->
                    buildStatusException(
                        Status.INVALID_ARGUMENT,
                        "BusinessError",
                        ex.toErrorDetail(),
                    )
                else -> {
                    val correlationId = UUID.randomUUID().toString()
                    logger.error(ex) { "予期せぬエラー correlationId=$correlationId" }
                    buildStatusException(
                        Status.UNKNOWN,
                        "UnknownError",
                        unknownErrorDetail(correlationId),
                    )
                }
            }
        }
}
