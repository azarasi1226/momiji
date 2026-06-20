"use server";

import { toSimpleActionError } from "@/lib/action-utils";
import { PreparePaymentService } from "@/grpc/gen/momiji/order/preparepayment/preparepayment_pb.js";
import { StartOrderService } from "@/grpc/gen/momiji/order/start/start_pb.js";
import { createGrpcClient } from "@/lib/grpc";
import { redirectIfUnauthenticated } from "@/lib/grpc-error";
import { requireValidSession } from "@/lib/session";

export type StartOrderState = {
  success?: boolean;
  orderId?: string;
  error?: string;
} | null;

/**
 * 注文を開始する（在庫予約）。 注文明細は server がカゴから読むので、 ここで送るのは配送先 ID と
 * 「画面で見ていた合計金額」だけ。 合計は server の権威価格と突き合わされ、 食い違えば弾かれる。
 */
export async function startOrder(
  shippingAddressId: string,
  expectedTotalAmount: number,
): Promise<StartOrderState> {
  const session = await requireValidSession();
  try {
    const client = createGrpcClient(StartOrderService, session.accessToken);
    const res = await client.startOrder({
      shippingAddressId,
      // proto は int64 → bigint。 合計金額（円）を渡す。
      expectedTotalAmount: BigInt(expectedTotalAmount),
    });
    return { success: true, orderId: res.orderId };
  } catch (e) {
    redirectIfUnauthenticated(e);
    return toSimpleActionError(e);
  }
}

export type PreparePaymentState = {
  success?: boolean;
  clientSecret?: string;
  error?: string;
} | null;

/**
 * 注文の決済を準備する（PaymentIntent 作成）。 成功すると client_secret が返り、 フロントの Stripe.js で
 * confirm（3DS）する。 注文・カードの所有権は server 側で検証される。
 */
export async function preparePayment(
  orderId: string,
  paymentMethodId: string,
): Promise<PreparePaymentState> {
  const session = await requireValidSession();
  try {
    const client = createGrpcClient(PreparePaymentService, session.accessToken);
    const res = await client.preparePayment({ orderId, paymentMethodId });
    return { success: true, clientSecret: res.clientSecret };
  } catch (e) {
    redirectIfUnauthenticated(e);
    return toSimpleActionError(e);
  }
}
