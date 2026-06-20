"use server";

import { revalidatePath } from "next/cache";
import { ClearBasketService } from "@/grpc/gen/momiji/basket/clear/clear_pb.js";
import { DeleteBasketItemService } from "@/grpc/gen/momiji/basket/deleteitem/deleteitem_pb.js";
import { SetBasketItemService } from "@/grpc/gen/momiji/basket/setitem/setitem_pb.js";
import { toSimpleActionError } from "@/lib/action-utils";
import { createGrpcClient } from "@/lib/grpc";
import { redirectIfUnauthenticated } from "@/lib/grpc-error";
import { requireValidSession } from "@/lib/session";

export type BasketActionState = {
  success?: boolean;
  error?: string;
} | null;

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
    return toSimpleActionError(e, "処理に失敗しました");
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
    return toSimpleActionError(e, "処理に失敗しました");
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
    return toSimpleActionError(e, "処理に失敗しました");
  }
  revalidatePath("/shop/basket");
  return { success: true };
}
