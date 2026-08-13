import { FindMyBasketService } from "@/grpc/gen/momiji/basket/findmybasket/findmybasket_pb.js";
import { FindMyBasketSummaryService } from "@/grpc/gen/momiji/basket/findmybasketsummary/findmybasketsummary_pb.js";
import { createGrpcClient } from "@/lib/grpc";
import { redirectIfUnauthenticated } from "@/lib/grpc-error";
import { requireValidSession } from "@/lib/session";

export type BasketItem = {
  productId: string;
  productName: string;
  productPrice: number;
  productImageUrl: string;
  itemQuantity: number;
};

export type BasketPage = {
  items: BasketItem[];
  totalCount: number;
  totalPage: number;
  pageNumber: number;
};

export type BasketSummary = {
  /** 各商品の個数の合計（かごバッジ用）。 */
  totalQuantity: number;
  /** 商品の種類数。 */
  totalTypeCount: number;
  /** 合計金額（単価 × 個数 の総和）。 */
  totalPrice: number;
};

/**
 * カゴの集計値のみを取得する。 items を取らず backend 側で SUM するので、
 * かごの上限やページングをフロントで意識しなくてよい。
 */
export async function findBasketSummary(): Promise<BasketSummary> {
  const session = await requireValidSession();
  try {
    const client = createGrpcClient(
      FindMyBasketSummaryService,
      session.accessToken,
    );
    const res = await client.findMyBasketSummary({});
    return {
      totalQuantity: res.totalQuantity,
      totalTypeCount: res.totalTypeCount,
      totalPrice: Number(res.totalPrice),
    };
  } catch (e) {
    redirectIfUnauthenticated(e);
    throw e;
  }
}

export async function findBasket(params: {
  pageSize?: number;
  pageNumber?: number;
}): Promise<BasketPage> {
  const session = await requireValidSession();
  try {
    const client = createGrpcClient(FindMyBasketService, session.accessToken);
    const res = await client.findMyBasket({
      pageSize: params.pageSize ?? 0,
      pageNumber: params.pageNumber ?? 0,
    });
    return {
      items: res.items.map((i) => ({
        productId: i.productId,
        productName: i.productName,
        productPrice: i.productPrice,
        productImageUrl: i.productImageUrl ?? "",
        itemQuantity: i.itemQuantity,
      })),
      totalCount: Number(res.paging?.totalCount ?? 0),
      totalPage: res.paging?.totalPage ?? 0,
      pageNumber: res.paging?.pageNumber ?? 0,
    };
  } catch (e) {
    redirectIfUnauthenticated(e);
    throw e;
  }
}
