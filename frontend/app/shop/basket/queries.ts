import { FindBasketByIdService } from "@/grpc/gen/momiji/basket/findbyid/findbyid_pb.js";
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
