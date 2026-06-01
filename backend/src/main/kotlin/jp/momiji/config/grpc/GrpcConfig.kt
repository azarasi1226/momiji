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
import org.springframework.security.oauth2.jwt.JwtDecoder
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
     * このプロジェクトでは JwtDecoder は gRPC 認証 ([GrpcAuthInterceptor]) でしか使わないので
     * Security 系の独立 Config を分けずにこちらに置く。
     */
    @Bean
    fun jwtDecoder(
        @Value("\${momiji.oidc.issuer-uri}") issuerUri: String,
    ): JwtDecoder =
        NimbusJwtDecoder
            .withIssuerLocation(issuerUri)
            .build()

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
     * - [BusinessException]    → `INVALID_ARGUMENT` + UseCaseError (message 1つ)
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
                        ex.message ?: "validation error",
                        ex.toErrorDetail(),
                    )
                is BusinessException ->
                    buildStatusException(
                        Status.INVALID_ARGUMENT,
                        ex.error.message,
                        ex.toErrorDetail(),
                    )
                else -> {
                    val correlationId = UUID.randomUUID().toString()
                    logger.error(ex) { "予期せぬエラー correlationId=$correlationId" }
                    buildStatusException(
                        Status.UNKNOWN,
                        "サーバーエラーが発生しました",
                        unknownErrorDetail(correlationId),
                    )
                }
            }
        }
}
