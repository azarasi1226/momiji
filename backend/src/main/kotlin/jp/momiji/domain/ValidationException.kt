package jp.momiji.domain

/**
 * 値オブジェクト validation の集約エラーを gRPC 層に運ぶための例外。
 *
 * gRPC 入口で `domain` の各 value object を組み立てて、 失敗したものを全部この例外に乗せる。
 * `GrpcConfig.grpcExceptionHandler` が `INVALID_ARGUMENT` にマッピングする。
 */
class ValidationException(
    val errors: List<DomainError>,
) : RuntimeException(
        errors.joinToString(separator = " / ") { "[${it.field}] ${it.message}" },
    )
