package jp.momiji.config.grpc

import io.grpc.ServerInterceptor
import io.grpc.Status
import io.grpc.StatusException
import jp.momiji.domain.UseCaseException
import jp.momiji.domain.ValidationException
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.grpc.server.GlobalServerInterceptor
import org.springframework.grpc.server.exception.GrpcExceptionHandler
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder

/**
 * gRPCサーバーの横断設定。
 *
 * - 認証Interceptor（全gRPCコールにJWT検証をかける）
 * - 例外ハンドラ（業務例外 → gRPC Status への変換）
 *
 * を Bean として提供する。`spring-grpc-spring-boot-starter` が起動時にこれらを拾い上げ、
 * 自動構成されるgRPCサーバに組み込む。
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
     * ハンドラ内で投げた業務例外を、適切な gRPC Status にマッピングするハンドラ。
     *
     * 現状の方針:
     *   - [UseCaseException] （バリデーション失敗・ビジネスルール違反） → `INVALID_ARGUMENT`
     *   - その他の例外は `null` を返して spring-grpc 既定の処理（`UNKNOWN` 等）に委ねる
     *
     * 改善余地: 「ユーザー未存在」「メール重複」など意味別に `NOT_FOUND` / `ALREADY_EXISTS` /
     * `PERMISSION_DENIED` に分けたほうが、クライアント側でハンドリングしやすい。
     */
    @Bean
    fun grpcExceptionHandler(): GrpcExceptionHandler =
        GrpcExceptionHandler { ex ->
            when (ex) {
                is ValidationException -> StatusException(Status.INVALID_ARGUMENT.withDescription(ex.message))
                is UseCaseException -> StatusException(Status.INVALID_ARGUMENT.withDescription(ex.error.message))
                else -> null
            }
        }
}
