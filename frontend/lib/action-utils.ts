import { parseConnectError } from "@/lib/grpc-error";

export type ActionErrorState = {
  error?: string;
  fieldErrors?: Record<string, string>;
};

/** フォーム用: ValidationError はフィールド別エラーとして返す */
export function toActionError(e: unknown): ActionErrorState {
  const parsed = parseConnectError(e);
  if (parsed?.fieldErrors) return { fieldErrors: parsed.fieldErrors };
  if (parsed?.businessError) return { error: parsed.businessError };
  if (parsed?.unknownError) {
    throw new Error(
      `${parsed.unknownError.message} (問い合わせ番号: ${parsed.unknownError.correlationId})`,
    );
  }
  if (parsed?.fallback) throw new Error(parsed.fallback);
  throw new Error("予期しないエラーが発生しました");
}

/** 非フォーム用: ValidationError も単一メッセージに畳む */
export function toSimpleActionError(e: unknown): { error: string } {
  const parsed = parseConnectError(e);
  if (parsed?.fieldErrors) {
    return {
      error: Object.values(parsed.fieldErrors)[0] ?? "入力値が不正です",
    };
  }
  if (parsed?.businessError) return { error: parsed.businessError };
  if (parsed?.unknownError) {
    throw new Error(
      `${parsed.unknownError.message} (問い合わせ番号: ${parsed.unknownError.correlationId})`,
    );
  }
  if (parsed?.fallback) throw new Error(parsed.fallback);
  throw new Error("予期しないエラーが発生しました");
}
