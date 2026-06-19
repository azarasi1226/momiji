"use server";

import { revalidatePath } from "next/cache";
import type { CancellationReason } from "@/grpc/gen/momiji/order/cancel/cancel_pb.js";
import { CancelOrderService } from "@/grpc/gen/momiji/order/cancel/cancel_pb.js";
import { createGrpcClient } from "@/lib/grpc";
import { parseConnectError, redirectIfUnauthenticated } from "@/lib/grpc-error";
import { requireValidSession } from "@/lib/session";

export type CancelOrderState = { error?: string } | null;

/**
 * 注文をキャンセルする（発送前のみ）。 理由は必須。 課金済みなら backend が返金する。
 * 成功時は詳細・一覧を revalidate する（ステータス反映は projection が非同期なので、 表示は少し遅れることがある）。
 */
export async function cancelOrder(
  orderId: string,
  reason: CancellationReason,
): Promise<CancelOrderState> {
  const session = await requireValidSession();
  try {
    const client = createGrpcClient(CancelOrderService, session.accessToken);
    await client.cancelOrder({ orderId, reason });
  } catch (e) {
    redirectIfUnauthenticated(e);
    const parsed = parseConnectError(e);
    if (parsed?.businessError) return { error: parsed.businessError };
    return { error: "注文のキャンセルに失敗しました" };
  }
  revalidatePath(`/shop/orders/${orderId}`);
  revalidatePath("/shop/orders");
  return null;
}
