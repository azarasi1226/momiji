import { FindProductByIdService } from "@/grpc/gen/momiji/product/findbyid/v1/findbyid_pb.js";
import { ListProductsService } from "@/grpc/gen/momiji/product/list/v1/list_pb.js";
import { ProductSortCondition } from "@/grpc/gen/momiji/product/v1/sort_pb.js";
import { ProductStatus } from "@/grpc/gen/momiji/product/v1/status_pb.js";
import { FindStockByProductIdService } from "@/grpc/gen/momiji/stock/findbyproductid/v1/findbyproductid_pb.js";
import { createGrpcClient } from "@/lib/grpc";
import { redirectIfUnauthenticated } from "@/lib/grpc-error";
import { requireValidSession } from "@/lib/session";

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

export type ShopProductDetail = {
  id: string;
  name: string;
  description: string;
  imageUrl: string;
  price: number;
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
