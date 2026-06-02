import { Code, ConnectError } from "@connectrpc/connect"
import { ErrorDetailSchema } from "@/grpc/gen/momiji/common/v1/error_pb.js"

/**
 * 構造化エラーを backend から取り出す共通パーサ。
 *
 * backend は ErrorDetail を gRPC Status.details に乗せて返す:
 * - businessError: ビジネスルール違反 (例: ユーザー未存在)
 * - validationError: 値オブジェクト validation の集約エラー
 * - unknownError: 想定外例外 (固定メッセージ + correlationId)
 *
 * 返り値:
 * - businessError: 表示用文字列
 * - fieldErrors: field 名 → message のマップ (form の field 別ハイライト用)
 * - unknownError: 固定メッセージ + correlationId (backend ログ突合用)
 * - fallback: details が無い場合の生メッセージ
 */
export type ParsedConnectError = {
  businessError?: string
  fieldErrors?: Record<string, string>
  unknownError?: { message: string; correlationId: string }
  fallback?: string
}

export function parseConnectError(e: unknown): ParsedConnectError | null {
  if (!(e instanceof ConnectError)) return null
  const details = e.findDetails(ErrorDetailSchema)
  if (details.length === 0) return { fallback: e.message }
  const detail = details[0]
  if (detail.error.case === "businessError") {
    return { businessError: detail.error.value.message }
  }
  if (detail.error.case === "validationError") {
    const fieldErrors: Record<string, string> = {}
    for (const fe of detail.error.value.errors) {
      fieldErrors[fe.fieldName] = fe.message
    }
    return { fieldErrors }
  }
  if (detail.error.case === "unknownError") {
    return {
      unknownError: {
        message: detail.error.value.message,
        correlationId: detail.error.value.correlationId,
      },
    }
  }
  return { fallback: e.message }
}

/**
 * gRPC エラーを人間が読める形 (gRPC code 名 / メッセージ / correlationId) に整形する。
 * エラーページ表示やログで「実際に何が起きたか」を見せるための共通フォーマッタ。
 */
export function formatConnectError(e: unknown): {
  code?: string
  message: string
  correlationId?: string
} {
  if (!(e instanceof ConnectError)) {
    return { message: e instanceof Error ? e.message : String(e) }
  }

  const code = Code[e.code]
  const parsed = parseConnectError(e)

  if (parsed?.unknownError) {
    return { code, message: parsed.unknownError.message, correlationId: parsed.unknownError.correlationId }
  }
  if (parsed?.businessError) {
    return { code, message: parsed.businessError }
  }
  if (parsed?.fieldErrors) {
    const message = Object.entries(parsed.fieldErrors)
      .map(([field, msg]) => `${field}: ${msg}`)
      .join(" / ")
    return { code, message }
  }
  return { code, message: parsed?.fallback ?? e.message }
}
