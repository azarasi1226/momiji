"use server";

import { timestampDate } from "@bufbuild/protobuf/wkt";
import { revalidatePath } from "next/cache";
import { ShipOrderService } from "@/grpc/gen/momiji/order/ship/ship_pb.js";
import { ListShippableOrdersService } from "@/grpc/gen/momiji/order/shippable/shippable_pb.js";
import { createGrpcClient } from "@/lib/grpc";
import { parseConnectError, redirectIfUnauthenticated } from "@/lib/grpc-error";
import { requireValidSession } from "@/lib/session";

export type ShippableOrder = {
  orderId: string;
  shippingAddress: {
    recipientName: string;
    phoneNumber: string;
    postalCode: string;
    prefecture: string;
    city: string;
    streetAddress: string;
    building: string;
    deliveryNote: string;
  };
  totalAmount: number;
  createdAt: string;
  items: { name: string; quantity: number }[];
};

/** 発送待ち（PAID）の注文一覧を取得する。 */
export async function listShippableOrders(): Promise<ShippableOrder[]> {
  const session = await requireValidSession();
  try {
    const client = createGrpcClient(
      ListShippableOrdersService,
      session.accessToken,
    );
    const res = await client.listShippableOrders({});
    return res.orders.map((o) => ({
      orderId: o.orderId,
      shippingAddress: {
        recipientName: o.shippingAddress?.recipientName ?? "",
        phoneNumber: o.shippingAddress?.phoneNumber ?? "",
        postalCode: o.shippingAddress?.postalCode ?? "",
        prefecture: o.shippingAddress?.prefecture ?? "",
        city: o.shippingAddress?.city ?? "",
        streetAddress: o.shippingAddress?.streetAddress ?? "",
        building: o.shippingAddress?.building ?? "",
        deliveryNote: o.shippingAddress?.deliveryNote ?? "",
      },
      // total_amount は int64 → proto-es では bigint。 表示用に number へ。
      totalAmount: Number(o.totalAmount),
      createdAt: o.createdAt ? timestampDate(o.createdAt).toISOString() : "",
      items: o.items.map((i) => ({ name: i.name, quantity: i.quantity })),
    }));
  } catch (e) {
    redirectIfUnauthenticated(e);
    throw e;
  }
}

export type ShipOrderResult =
  | { success: true }
  | { success: false; error: string };

function toErrorMessage(e: unknown): string {
  const parsed = parseConnectError(e);
  if (parsed?.businessError) return parsed.businessError;
  if (parsed?.fieldErrors) {
    return Object.values(parsed.fieldErrors)[0] ?? "入力値が不正です";
  }
  if (parsed?.unknownError) {
    return `${parsed.unknownError.message} (問い合わせ番号: ${parsed.unknownError.correlationId})`;
  }
  if (parsed?.fallback) return parsed.fallback;
  return "発送に失敗しました";
}

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
    return { success: false, error: toErrorMessage(e) };
  }
  revalidatePath("/admin/orders");
  return { success: true };
}
