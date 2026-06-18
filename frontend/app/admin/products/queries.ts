import { timestampDate } from "@bufbuild/protobuf/wkt";
import { FindProductByIdService } from "@/grpc/gen/momiji/product/findbyid/v1/findbyid_pb.js";
import { ListProductsService } from "@/grpc/gen/momiji/product/list/v1/list_pb.js";
import { ProductSortCondition } from "@/grpc/gen/momiji/product/v1/sort_pb.js";
import { ProductStatus } from "@/grpc/gen/momiji/product/v1/status_pb.js";
import { FindStockByProductIdService } from "@/grpc/gen/momiji/stock/findbyproductid/v1/findbyproductid_pb.js";
import { createGrpcClient } from "@/lib/grpc";
import { redirectIfUnauthenticated } from "@/lib/grpc-error";
import { requireValidSession } from "@/lib/session";

export type Product = {
  id: string;
  brandId: string;
  name: string;
  description: string;
  imageUrl: string;
  price: number;
  status: string;
  createdAt: string;
  updatedAt: string;
};

export type ProductListItem = Product & {
  stockOnHand: number;
  stockReserved: number;
  stockAvailable: number;
};

export type ProductsPage = {
  products: ProductListItem[];
  totalCount: number;
  totalPage: number;
  pageSize: number;
  pageNumber: number;
};

export type ListProductsParams = {
  likeName?: string;
  status?: string;
  brandId?: string;
  sort?: string;
  pageSize?: number;
  pageNumber?: number;
};

export type Stock = {
  onHand: number;
  reserved: number;
  available: number;
};

const SORT_MAP: Record<string, ProductSortCondition> = {
  name_asc: ProductSortCondition.NAME_ASC,
  name_desc: ProductSortCondition.NAME_DESC,
  price_asc: ProductSortCondition.PRICE_ASC,
  price_desc: ProductSortCondition.PRICE_DESC,
  created_desc: ProductSortCondition.CREATED_AT_DESC,
  created_asc: ProductSortCondition.CREATED_AT_ASC,
};

const STATUS_FILTER_MAP: Record<string, ProductStatus> = {
  ACTIVE: ProductStatus.ACTIVE,
  DISCONTINUED: ProductStatus.DISCONTINUED,
};

function productStatusName(status: ProductStatus): string {
  switch (status) {
    case ProductStatus.ACTIVE:
      return "ACTIVE";
    case ProductStatus.DISCONTINUED:
      return "DISCONTINUED";
    default:
      return "UNKNOWN";
  }
}

export async function listProducts(
  params: ListProductsParams = {},
): Promise<ProductsPage> {
  const session = await requireValidSession();
  try {
    const client = createGrpcClient(ListProductsService, session.accessToken);
    const res = await client.listProducts({
      likeName: params.likeName ?? "",
      status:
        STATUS_FILTER_MAP[params.status ?? ""] ?? ProductStatus.UNSPECIFIED,
      brandId: params.brandId ?? "",
      sort: SORT_MAP[params.sort ?? ""] ?? ProductSortCondition.UNSPECIFIED,
      pageSize: params.pageSize ?? 0,
      pageNumber: params.pageNumber ?? 0,
    });
    return {
      products: res.products.map((p) => ({
        id: p.id,
        brandId: p.brandId,
        name: p.name,
        description: p.description,
        imageUrl: p.imageUrl ?? "",
        price: p.price,
        status: productStatusName(p.status),
        createdAt: p.createdAt ? timestampDate(p.createdAt).toISOString() : "",
        updatedAt: p.updatedAt ? timestampDate(p.updatedAt).toISOString() : "",
        stockOnHand: p.stockOnHand,
        stockReserved: p.stockReserved,
        stockAvailable: p.stockAvailable,
      })),
      totalCount: Number(res.paging?.totalCount ?? 0),
      totalPage: res.paging?.totalPage ?? 0,
      pageSize: res.paging?.pageSize ?? 0,
      pageNumber: res.paging?.pageNumber ?? 0,
    };
  } catch (e) {
    redirectIfUnauthenticated(e);
    throw e;
  }
}

export async function fetchProduct(id: string): Promise<Product> {
  const session = await requireValidSession();
  try {
    const client = createGrpcClient(
      FindProductByIdService,
      session.accessToken,
    );
    const res = await client.findProductById({ id });
    return {
      id: res.id,
      brandId: res.brandId,
      name: res.name,
      description: res.description,
      imageUrl: res.imageUrl ?? "",
      price: res.price,
      status: productStatusName(res.status),
      createdAt: res.createdAt ? timestampDate(res.createdAt).toISOString() : "",
      updatedAt: res.updatedAt ? timestampDate(res.updatedAt).toISOString() : "",
    };
  } catch (e) {
    redirectIfUnauthenticated(e);
    throw e;
  }
}

export async function fetchStock(productId: string): Promise<Stock> {
  const session = await requireValidSession();
  try {
    const client = createGrpcClient(
      FindStockByProductIdService,
      session.accessToken,
    );
    const res = await client.findStockByProductId({ productId });
    return {
      onHand: res.onHand,
      reserved: res.reserved,
      available: res.available,
    };
  } catch (e) {
    redirectIfUnauthenticated(e);
    throw e;
  }
}
