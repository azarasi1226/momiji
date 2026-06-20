"use server";

import { revalidatePath } from "next/cache";
import { ShipOrderService } from "@/grpc/gen/momiji/order/ship/ship_pb.js";
import { toSimpleActionError } from "@/lib/action-utils";
import { createGrpcClient } from "@/lib/grpc";
import { redirectIfUnauthenticated } from "@/lib/grpc-error";
import { requireValidSession } from "@/lib/session";

export type { ShippableOrder } from "./queries";

export type ShipOrderResult =
  | { success: true }
  | { success: false; error: string };

/**
 * 注文を発送済みにする（PAID → SHIPPED）。 完了は backend の reactor が自動で続ける。
 * 失敗はボタンに表示するため throw せず結果で返す（未認証だけは redirect で抜ける）。
 */
export async function shipOrder(orderId: string): Promise<ShipOrderResult> {
  const session = await requireValidSession();
  try {
    const client = createGrpcClient(ShipOrderService, session.accessToken);
    await client.shipOrder({ orderId });
  } catch (e) {
    redirectIfUnauthenticated(e);
    return { success: false, error: toSimpleActionError(e, "発送に失敗しました").error };
  }
  revalidatePath("/admin/orders");
  return { success: true };
}
