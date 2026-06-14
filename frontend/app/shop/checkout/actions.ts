"use server"

import { createGrpcClient } from "@/lib/grpc"
import { requireValidSession } from "@/lib/session"
import { redirectIfUnauthenticated, parseConnectError } from "@/lib/grpc-error"
import { StartOrderService } from "@/grpc/gen/momiji/order/start/v1/start_pb.js"

export type StartOrderState = {
  success?: boolean
  orderId?: string
  error?: string
} | null

/**
 * 注文を開始する（在庫予約）。 注文明細は server がカゴから読むので、 ここで送るのは配送先 ID と
 * 「画面で見ていた合計金額」だけ。 合計は server の権威価格と突き合わされ、 食い違えば弾かれる。
 */
export async function startOrder(
  shippingAddressId: string,
  expectedTotalAmount: number,
): Promise<StartOrderState> {
  const session = await requireValidSession()
  try {
    const client = createGrpcClient(StartOrderService, session.accessToken)
    const res = await client.startOrder({
      shippingAddressId,
      // proto は int64 → bigint。 合計金額（円）を渡す。
      expectedTotalAmount: BigInt(expectedTotalAmount),
    })
    return { success: true, orderId: res.orderId }
  } catch (e) {
    redirectIfUnauthenticated(e)
    const parsed = parseConnectError(e)
    if (parsed?.businessError) return { error: parsed.businessError }
    if (parsed?.fieldErrors) {
      return { error: Object.values(parsed.fieldErrors)[0] ?? "入力値が不正です" }
    }
    if (parsed?.unknownError) {
      return {
        error: `${parsed.unknownError.message} (問い合わせ番号: ${parsed.unknownError.correlationId})`,
      }
    }
    return { error: parsed?.fallback ?? "注文の開始に失敗しました" }
  }
}
