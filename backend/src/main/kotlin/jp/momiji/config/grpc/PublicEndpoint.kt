package jp.momiji.config.grpc

/**
 * このアノテーションが付いた gRPC メソッドは、認証 ([GrpcAuthInterceptor]) をスキップする。
 *
 * 用途: Health Check / Reflection / 認証前に呼びたいAPI（公開ログイン情報取得など）。
 *
 * 使用例:
 * ```
 * @PublicEndpoint
 * override suspend fun healthCheck(request: ...): ... { ... }
 * ```
 *
 * 検出は [PublicEndpointRegistry] が起動時に行う。
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class PublicEndpoint
