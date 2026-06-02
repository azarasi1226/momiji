package jp.momiji.config.grpc

import com.google.protobuf.Any
import io.grpc.Status
import io.grpc.StatusException
import io.grpc.protobuf.StatusProto
import jp.momiji.domain.BusinessException
import jp.momiji.domain.ValidationException
import jp.momiji.grpc.momiji.common.v1.BusinessError
import jp.momiji.grpc.momiji.common.v1.ErrorDetail
import jp.momiji.grpc.momiji.common.v1.FieldError
import jp.momiji.grpc.momiji.common.v1.UnknownError
import jp.momiji.grpc.momiji.common.v1.ValidationError
import com.google.rpc.Status as RpcStatus

/**
 * Domain 例外 → gRPC 構造化エラー ([ErrorDetail]) への変換ロジック群。
 *
 * 拡張関数として書く理由:
 * - 呼び出し側 ([GrpcConfig.grpcExceptionHandler]) が `ex.toErrorDetail()` で素直に書ける
 * - domain 層は gRPC を知らずに済む (依存方向: config → domain)
 *
 * `internal` 修飾で同モジュール外には公開しない (拡張点は GrpcConfig だけ)。
 */

internal fun ValidationException.toErrorDetail(): ErrorDetail =
    ErrorDetail
        .newBuilder()
        .setValidationError(
            ValidationError
                .newBuilder()
                .addAllErrors(
                    errors.map { err ->
                        FieldError
                            .newBuilder()
                            .setFieldName(err.field)
                            .setMessage(err.message)
                            .build()
                    },
                ).build(),
        ).build()

internal fun BusinessException.toErrorDetail(): ErrorDetail =
    ErrorDetail
        .newBuilder()
        .setBusinessError(
            BusinessError
                .newBuilder()
                .setMessage(error.message)
                .build(),
        ).build()

/**
 * 想定外例外用の構造化エラー。
 *
 * 内部実装情報 (SQL カラム名 / 内部ホスト名 / ファイルパス 等) を漏らさないため、
 * message は固定文字列で、 詳細は backend のログにだけ出す設計 (ADR 0002)。
 * クライアントは [correlationId] をサポートに伝えてサーバーログ突合する運用。
 *
 * `Throwable` 拡張ではなく独立関数にしている理由: 入力は correlationId だけで、
 * 例外オブジェクト自体は使わないため、 拡張関数の利点が無い。
 */
internal fun unknownErrorDetail(correlationId: String): ErrorDetail =
    ErrorDetail
        .newBuilder()
        .setUnknownError(
            UnknownError
                .newBuilder()
                .setMessage("予期せぬエラーが発生しました。　サポートにお問い合わせの際、以下のエラーIDをお伝えください。")
                .setCorrelationId(correlationId)
                .build(),
        ).build()

/**
 * gRPC `Status` + `ErrorDetail` を 1 つの [StatusException] にパッケージする helper。
 *
 * `google.rpc.Status` のフィールド `details` に `Any.pack(errorDetail)` で添付し、
 * `StatusProto.toStatusException(...)` で gRPC stack が運べる形にする。
 */
internal fun buildStatusException(
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
