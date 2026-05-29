package jp.momiji.config.grpc

import com.google.protobuf.Any
import io.github.oshai.kotlinlogging.KotlinLogging
import io.grpc.ServerInterceptor
import io.grpc.Status
import io.grpc.StatusException
import io.grpc.protobuf.StatusProto
import jp.momiji.domain.UseCaseException
import jp.momiji.domain.ValidationException
import jp.momiji.grpc.momiji.common.v1.ErrorDetail
import jp.momiji.grpc.momiji.common.v1.FieldError
import jp.momiji.grpc.momiji.common.v1.UnknownError
import jp.momiji.grpc.momiji.common.v1.UseCaseError
import jp.momiji.grpc.momiji.common.v1.ValidationError
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.grpc.server.GlobalServerInterceptor
import org.springframework.grpc.server.exception.GrpcExceptionHandler
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import java.util.UUID
import com.google.rpc.Status as RpcStatus

private val logger = KotlinLogging.logger {}

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
     * ハンドラ内で投げた業務例外を、 gRPC Status + 構造化 details にマッピングするハンドラ。
     *
     * - [UseCaseException] → `INVALID_ARGUMENT` + [ErrorDetail.use_case_error] (message 1つ)
     * - [ValidationException] → `INVALID_ARGUMENT` + [ErrorDetail.validation_error] (field 別エラーリスト)
     * - その他は `UNKNOWN` + [ErrorDetail.unknown_error] でクライアントが「予期せぬエラー」 として表示できるようにする
     *
     * フロント側は ConnectError.findDetails(ErrorDetailSchema) で type-safe に取り出せる。
     */
    @Bean
    fun grpcExceptionHandler(): GrpcExceptionHandler =
        GrpcExceptionHandler { ex ->
            when (ex) {
                is ValidationException ->
                    buildStatusException(
                        Status.INVALID_ARGUMENT,
                        ex.message ?: "validation error",
                        buildValidationDetail(ex),
                    )
                is UseCaseException ->
                    buildStatusException(
                        Status.INVALID_ARGUMENT,
                        ex.error.message,
                        buildUseCaseDetail(ex),
                    )
                else -> {
                    // 想定外例外: 内部情報をクライアントに漏らさないため、
                    // 詳細はサーバーログにだけ書き、 クライアントには correlationId を渡して
                    // 後でサポート側でログ突合できるようにする。
                    val correlationId = UUID.randomUUID().toString()
                    logger.error(ex) { "予期せぬエラー correlationId=$correlationId" }
                    buildStatusException(
                        Status.UNKNOWN,
                        "サーバーエラーが発生しました",
                        buildUnknownDetail(correlationId),
                    )
                }
            }
        }

    private fun buildValidationDetail(ex: ValidationException): ErrorDetail =
        ErrorDetail
            .newBuilder()
            .setValidationError(
                ValidationError
                    .newBuilder()
                    .addAllErrors(
                        ex.errors.map { err ->
                            FieldError
                                .newBuilder()
                                .setFieldName(err.field)
                                .setMessage(err.message)
                                .build()
                        },
                    ).build(),
            ).build()

    private fun buildUseCaseDetail(ex: UseCaseException): ErrorDetail =
        ErrorDetail
            .newBuilder()
            .setUseCaseError(
                UseCaseError
                    .newBuilder()
                    .setMessage(ex.error.message)
                    .build(),
            ).build()

    /**
     * 想定外例外を構造化する。 内部実装情報 (SQL カラム名、 内部ホスト名、 ファイルパス 等) を
     * クライアントに漏らさないため、 message は固定文字列、 correlationId はサポート問合せ用 UUID。
     */
    private fun buildUnknownDetail(correlationId: String): ErrorDetail =
        ErrorDetail
            .newBuilder()
            .setUnknownError(
                UnknownError
                    .newBuilder()
                    .setMessage("サーバーエラーが発生しました")
                    .setCorrelationId(correlationId)
                    .build(),
            ).build()

    private fun buildStatusException(
        status: Status,
        message: String,
        detail: ErrorDetail,
    ): StatusException {
        val rpcStatus =
            RpcStatus
                .newBuilder()
                .setCode(status.code.value())
                .setMessage(message)
                .addDetails(Any.pack(detail))
                .build()
        return StatusProto.toStatusException(rpcStatus)
    }
}
