"use server";

import { revalidatePath } from "next/cache";
import { ClearBasketService } from "@/grpc/gen/momiji/basket/clear/v1/clear_pb.js";
import { DeleteBasketItemService } from "@/grpc/gen/momiji/basket/deleteitem/v1/deleteitem_pb.js";
import { SetBasketItemService } from "@/grpc/gen/momiji/basket/setitem/v1/setitem_pb.js";
import { createGrpcClient } from "@/lib/grpc";
import { parseConnectError, redirectIfUnauthenticated } from "@/lib/grpc-error";
import { requireValidSession } from "@/lib/session";

export type BasketActionState = {
  success?: boolean;
  error?: string;
} | null;

function toErrorState(e: unknown): BasketActionState {
  const parsed = parseConnectError(e);
  if (parsed?.fieldErrors) {
    return {
      error: Object.values(parsed.fieldErrors)[0] ?? "入力値が不正です",
    };
  }
  if (parsed?.businessError) return { error: parsed.businessError };
  if (parsed?.unknownError) {
    return {
      error: `${parsed.unknownError.message} (問い合わせ番号: ${parsed.unknownError.correlationId})`,
    };
  }
  if (parsed?.fallback) return { error: parsed.fallback };
  return { error: "処理に失敗しました" };
}

/**
 * カゴに商品をセット（追加 or 個数変更）。 個数は**絶対値**（加算ではない）。
 * 商品一覧の「カゴに入れる」と、カゴ画面の個数更新の両方から使う。
 */
export async function setBasketItem(
  productId: string,
  itemQuantity: number,
): Promise<BasketActionState> {
  const session = await requireValidSession();
  try {
    const client = createGrpcClient(SetBasketItemService, session.accessToken);
    await client.setBasketItem({ productId, itemQuantity });
  } catch (e) {
    redirectIfUnauthenticated(e);
    return toErrorState(e);
  }
  revalidatePath("/shop/basket");
  return { success: true };
}

export async function deleteBasketItem(
  productId: string,
): Promise<BasketActionState> {
  const session = await requireValidSession();
  try {
    const client = createGrpcClient(
      DeleteBasketItemService,
      session.accessToken,
    );
    await client.deleteBasketItem({ productId });
  } catch (e) {
    redirectIfUnauthenticated(e);
    return toErrorState(e);
  }
  revalidatePath("/shop/basket");
  return { success: true };
}

export async function clearBasket(): Promise<BasketActionState> {
  const session = await requireValidSession();
  try {
    const client = createGrpcClient(ClearBasketService, session.accessToken);
    await client.clearBasket({});
  } catch (e) {
    redirectIfUnauthenticated(e);
    return toErrorState(e);
  }
  revalidatePath("/shop/basket");
  return { success: true };
}
