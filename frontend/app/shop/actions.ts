"use server";

import { revalidatePath } from "next/cache";
import { ClearBasketService } from "@/grpc/gen/momiji/basket/clear/v1/clear_pb.js";
import { DeleteBasketItemService } from "@/grpc/gen/momiji/basket/deleteitem/v1/deleteitem_pb.js";
import { FindBasketByIdService } from "@/grpc/gen/momiji/basket/findbyid/v1/findbyid_pb.js";
import { SetBasketItemService } from "@/grpc/gen/momiji/basket/setitem/v1/setitem_pb.js";
import { FindProductByIdService } from "@/grpc/gen/momiji/product/findbyid/v1/findbyid_pb.js";
import { ListProductsService } from "@/grpc/gen/momiji/product/list/v1/list_pb.js";
import { ProductSortCondition } from "@/grpc/gen/momiji/product/v1/sort_pb.js";
import { ProductStatus } from "@/grpc/gen/momiji/product/v1/status_pb.js";
import { FindStockByProductIdService } from "@/grpc/gen/momiji/stock/findbyproductid/v1/findbyproductid_pb.js";
import { createGrpcClient } from "@/lib/grpc";
import { parseConnectError, redirectIfUnauthenticated } from "@/lib/grpc-error";
import { requireValidSession } from "@/lib/session";

// ── 商品一覧（購入者向け: ACTIVE のみ） ───────────────────────────────

export type ShopProduct = {
  id: string;
  name: string;
  description: string;
  imageUrl: string;
  price: number;
};

export type ShopProductsPage = {
  products: ShopProduct[];
  totalCount: number;
  totalPage: number;
  pageNumber: number;
};

const SORT_MAP: Record<string, ProductSortCondition> = {
  name_asc: ProductSortCondition.NAME_ASC,
  name_desc: ProductSortCondition.NAME_DESC,
  price_asc: ProductSortCondition.PRICE_ASC,
  price_desc: ProductSortCondition.PRICE_DESC,
  created_desc: ProductSortCondition.CREATED_AT_DESC,
  created_asc: ProductSortCondition.CREATED_AT_ASC,
};

export async function listShopProducts(params: {
  likeName?: string;
  sort?: string;
  inStockOnly?: boolean;
  pageSize?: number;
  pageNumber?: number;
}): Promise<ShopProductsPage> {
  const session = await requireValidSession();
  try {
    const client = createGrpcClient(ListProductsService, session.accessToken);
    const res = await client.listProducts({
      likeName: params.likeName ?? "",
      // 購入者は販売中（ACTIVE）の商品しか見えない。
      status: ProductStatus.ACTIVE,
      brandId: "",
      inStockOnly: params.inStockOnly ?? false,
      sort: SORT_MAP[params.sort ?? ""] ?? ProductSortCondition.UNSPECIFIED,
      pageSize: params.pageSize ?? 0,
      pageNumber: params.pageNumber ?? 0,
    });
    return {
      products: res.products.map((p) => ({
        id: p.id,
        name: p.name,
        description: p.description,
        imageUrl: p.imageUrl ?? "",
        price: p.price,
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

// ── 商品詳細（購入者向け） ──────────────────────────────────────────

export type ShopProductDetail = {
  id: string;
  name: string;
  description: string;
  imageUrl: string;
  price: number;
  // 販売中か（生産終了なら購入導線を出さない）。
  isActive: boolean;
};

export async function fetchShopProduct(id: string): Promise<ShopProductDetail> {
  const session = await requireValidSession();
  try {
    const client = createGrpcClient(
      FindProductByIdService,
      session.accessToken,
    );
    const res = await client.findProductById({ id });
    return {
      id: res.id,
      name: res.name,
      description: res.description,
      imageUrl: res.imageUrl ?? "",
      price: res.price,
      isActive: res.status === ProductStatus.ACTIVE,
    };
  } catch (e) {
    redirectIfUnauthenticated(e);
    throw e;
  }
}

export type ShopStock = {
  available: number;
};

/** 購入者向けの在庫状況。 表示・数量上限に使うのは販売可能数（available）。 */
export async function fetchShopStock(productId: string): Promise<ShopStock> {
  const session = await requireValidSession();
  try {
    const client = createGrpcClient(
      FindStockByProductIdService,
      session.accessToken,
    );
    const res = await client.findStockByProductId({ productId });
    return { available: res.available };
  } catch (e) {
    redirectIfUnauthenticated(e);
    throw e;
  }
}

// ── 買い物かご ─────────────────────────────────────────────────────

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

export async function findBasket(params: {
  pageSize?: number;
  pageNumber?: number;
}): Promise<BasketPage> {
  const session = await requireValidSession();
  try {
    const client = createGrpcClient(FindBasketByIdService, session.accessToken);
    const res = await client.findBasketById({
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
