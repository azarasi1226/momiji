package jp.momiji.domain

/**
 * [BusinessError] を gRPC 層に運ぶための例外。
 *
 * CommandHandler が返した `CommandResult.fail(...)` を、 gRPC 層が
 * [jp.momiji.feature.throwIfError] でこの例外に変換し、
 * `GrpcExceptionHandler` が `INVALID_ARGUMENT` にマッピングする。
 */
class UseCaseException(
    val error: BusinessError,
) : Exception("message:[${error.message}]")
