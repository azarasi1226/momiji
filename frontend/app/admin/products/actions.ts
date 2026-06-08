"use server"

import { Code, ConnectError } from "@connectrpc/connect"
import { timestampDate } from "@bufbuild/protobuf/wkt"
import { revalidatePath } from "next/cache"
import { redirect } from "next/navigation"
import { ulid } from "ulid"
import { createGrpcClient } from "@/lib/grpc"
import { requireValidSession } from "@/lib/session"
import { parseConnectError } from "@/lib/grpc-error"
import { listBrands } from "../brands/actions"
import { ListProductsService } from "@/grpc/gen/momiji/product/list/v1/list_pb.js"
import { FindProductByIdService } from "@/grpc/gen/momiji/product/findbyid/v1/findbyid_pb.js"
import { CreateProductService } from "@/grpc/gen/momiji/product/create/v1/create_pb.js"
import { UpdateProductService } from "@/grpc/gen/momiji/product/update/v1/update_pb.js"
import { DiscontinueProductService } from "@/grpc/gen/momiji/product/discontinue/v1/discontinue_pb.js"
import { ProductStatus } from "@/grpc/gen/momiji/product/v1/status_pb.js"
import { ProductSortCondition } from "@/grpc/gen/momiji/product/v1/sort_pb.js"
import { FindStockByProductIdService } from "@/grpc/gen/momiji/stock/findbyproductid/v1/findbyproductid_pb.js"
import { ReceiveStockService } from "@/grpc/gen/momiji/stock/receive/v1/receive_pb.js"
import { AdjustStockService } from "@/grpc/gen/momiji/stock/adjust/v1/adjust_pb.js"
import { StockAdjustmentReason } from "@/grpc/gen/momiji/stock/v1/reason_pb.js"
import { IssueImageUploadUrlService } from "@/grpc/gen/momiji/image/upload/v1/upload_pb.js"

export type Product = {
  id: string
  brandId: string
  name: string
  description: string
  imageUrl: string
  price: number
  status: string
  createdAt: string
  updatedAt: string
}

/** 一覧の 1 行（商品 + 在庫）。 在庫は read model の LEFT JOIN で、 無ければ 0。 */
export type ProductListItem = Product & {
  stockOnHand: number
  stockReserved: number
  stockAvailable: number
}

/** 一覧の 1 ページ分（商品 + ページング情報）。 */
export type ProductsPage = {
  products: ProductListItem[]
  totalCount: number
  totalPage: number
  pageSize: number
  pageNumber: number
}

/** 一覧クエリ。 すべて任意（未指定はサーバ既定）。 sort / status は UI 用の文字列キー。 */
export type ListProductsParams = {
  likeName?: string
  status?: string
  brandId?: string
  sort?: string
  pageSize?: number
  pageNumber?: number
}

/** UI の sort キー → proto enum。 不正/未指定は UNSPECIFIED（サーバが既定に倒す）。 */
const SORT_MAP: Record<string, ProductSortCondition> = {
  name_asc: ProductSortCondition.NAME_ASC,
  name_desc: ProductSortCondition.NAME_DESC,
  price_asc: ProductSortCondition.PRICE_ASC,
  price_desc: ProductSortCondition.PRICE_DESC,
  created_desc: ProductSortCondition.CREATED_AT_DESC,
  created_asc: ProductSortCondition.CREATED_AT_ASC,
}

/** UI の status フィルタキー → proto enum。 空/未指定は UNSPECIFIED（= すべて）。 */
const STATUS_FILTER_MAP: Record<string, ProductStatus> = {
  ACTIVE: ProductStatus.ACTIVE,
  DISCONTINUED: ProductStatus.DISCONTINUED,
}

/** proto enum (ProductStatus) を正準コード文字列に変換する（表示用ラベルは lib/status-labels.ts）。 */
function productStatusName(status: ProductStatus): string {
  switch (status) {
    case ProductStatus.ACTIVE:
      return "ACTIVE"
    case ProductStatus.DISCONTINUED:
      return "DISCONTINUED"
    default:
      return "UNKNOWN"
  }
}

/** Unauthenticated は session 切れなのでログインへ飛ばす。 */
function redirectIfUnauthenticated(e: unknown): void {
  if (e instanceof ConnectError && e.code === Code.Unauthenticated) {
    redirect("/")
  }
}

export async function listProducts(
  params: ListProductsParams = {},
): Promise<ProductsPage> {
  const session = await requireValidSession()
  try {
    const client = createGrpcClient(ListProductsService, session.accessToken)
    const res = await client.listProducts({
      likeName: params.likeName ?? "",
      status: STATUS_FILTER_MAP[params.status ?? ""] ?? ProductStatus.UNSPECIFIED,
      brandId: params.brandId ?? "",
      sort: SORT_MAP[params.sort ?? ""] ?? ProductSortCondition.UNSPECIFIED,
      pageSize: params.pageSize ?? 0,
      pageNumber: params.pageNumber ?? 0,
    })
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
    }
  } catch (e) {
    redirectIfUnauthenticated(e)
    throw e
  }
}

export async function fetchProduct(id: string): Promise<Product> {
  const session = await requireValidSession()
  try {
    const client = createGrpcClient(FindProductByIdService, session.accessToken)
    const res = await client.findProductById({ id })
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
    }
  } catch (e) {
    redirectIfUnauthenticated(e)
    throw e
  }
}

/**
 * 全ブランドの {id, name}（名前順）。 一覧の brandId→名前 解決と、 ブランドフィルタの選択肢に使う。
 * フィルタは archived 含む全ブランド対象（archived ブランドの商品も絞り込めるように）。
 */
export async function listAllBrands(): Promise<{ id: string; name: string }[]> {
  const brands = await listBrands()
  return brands.map((b) => ({ id: b.id, name: b.name }))
}

/** 作成フォームのドロップダウン用。 商品は ACTIVE なブランドにしか紐づけられない。 */
export async function listActiveBrands(): Promise<{ id: string; name: string }[]> {
  const brands = await listBrands()
  return brands
    .filter((b) => b.status === "ACTIVE")
    .map((b) => ({ id: b.id, name: b.name }))
}

export type ProductFormState = {
  success?: boolean
  error?: string
  fieldErrors?: Record<string, string>
} | null

function toErrorState(e: unknown): ProductFormState {
  const parsed = parseConnectError(e)
  if (parsed?.fieldErrors) return { fieldErrors: parsed.fieldErrors }
  if (parsed?.businessError) return { error: parsed.businessError }
  if (parsed?.unknownError) {
    return {
      error: `${parsed.unknownError.message} (問い合わせ番号: ${parsed.unknownError.correlationId})`,
    }
  }
  if (parsed?.fallback) return { error: parsed.fallback }
  return { error: "処理に失敗しました" }
}

export async function createProduct(
  _prevState: ProductFormState,
  formData: FormData,
): Promise<ProductFormState> {
  const session = await requireValidSession()
  const brandId = (formData.get("brandId") as string) ?? ""
  const name = (formData.get("name") as string) ?? ""
  const description = (formData.get("description") as string) ?? ""
  const imageUrl = (formData.get("imageUrl") as string) ?? ""
  const price = Number(formData.get("price") ?? 0)

  try {
    const client = createGrpcClient(CreateProductService, session.accessToken)
    // id は BFF 採番（冪等キー）。 image_url は空文字なら「画像なし」（サーバ側 VO が空→null に正規化）。
    await client.createProduct({
      id: ulid(),
      brandId,
      name,
      description,
      imageUrl,
      price,
    })
  } catch (e) {
    redirectIfUnauthenticated(e)
    return toErrorState(e)
  }

  revalidatePath("/admin/products")
  redirect("/admin/products")
}

export async function updateProduct(
  _prevState: ProductFormState,
  formData: FormData,
): Promise<ProductFormState> {
  const session = await requireValidSession()
  const id = formData.get("id") as string
  const name = (formData.get("name") as string) ?? ""
  const description = (formData.get("description") as string) ?? ""
  const imageUrl = (formData.get("imageUrl") as string) ?? ""
  const price = Number(formData.get("price") ?? 0)

  try {
    const client = createGrpcClient(UpdateProductService, session.accessToken)
    await client.updateProduct({
      id,
      name,
      description,
      imageUrl,
      price,
    })
  } catch (e) {
    redirectIfUnauthenticated(e)
    return toErrorState(e)
  }

  revalidatePath("/admin/products")
  revalidatePath(`/admin/products/${id}`)
  return { success: true }
}

export async function discontinueProduct(id: string): Promise<void> {
  const session = await requireValidSession()
  try {
    const client = createGrpcClient(DiscontinueProductService, session.accessToken)
    await client.discontinueProduct({ id })
  } catch (e) {
    redirectIfUnauthenticated(e)
    throw e
  }

  revalidatePath("/admin/products")
  redirect("/admin/products")
}

// ── 在庫 ───────────────────────────────────────────────────────────

export type Stock = {
  onHand: number
  reserved: number
  available: number
}

/** 商品の在庫状況。 在庫レコードが無い商品はサーバが暗黙ゼロを返す。 */
export async function fetchStock(productId: string): Promise<Stock> {
  const session = await requireValidSession()
  try {
    const client = createGrpcClient(FindStockByProductIdService, session.accessToken)
    const res = await client.findStockByProductId({ productId })
    return { onHand: res.onHand, reserved: res.reserved, available: res.available }
  } catch (e) {
    redirectIfUnauthenticated(e)
    throw e
  }
}

/** UI の理由キー → proto enum。 未知/未指定は UNSPECIFIED（サーバが弾く）。 */
const ADJUST_REASON_MAP: Record<string, StockAdjustmentReason> = {
  DAMAGED: StockAdjustmentReason.DAMAGED,
  LOST: StockAdjustmentReason.LOST,
  STOCKTAKING: StockAdjustmentReason.STOCKTAKING,
  CORRECTION: StockAdjustmentReason.CORRECTION,
  OTHER: StockAdjustmentReason.OTHER,
}

/** 入庫（在庫を増やす）。 quantity は正の数。 */
export async function receiveStock(
  _prevState: ProductFormState,
  formData: FormData,
): Promise<ProductFormState> {
  const session = await requireValidSession()
  const productId = formData.get("productId") as string
  const quantity = Number(formData.get("quantity") ?? 0)

  try {
    const client = createGrpcClient(ReceiveStockService, session.accessToken)
    await client.receiveStock({ productId, quantity })
  } catch (e) {
    redirectIfUnauthenticated(e)
    return toErrorState(e)
  }

  revalidatePath(`/admin/products/${productId}`)
  return { success: true }
}

/** 在庫調整（符号付き差分 + 理由）。 増加できるのは棚卸しのときだけ（サーバが検証）。 */
export async function adjustStock(
  _prevState: ProductFormState,
  formData: FormData,
): Promise<ProductFormState> {
  const session = await requireValidSession()
  const productId = formData.get("productId") as string
  const quantity = Number(formData.get("quantity") ?? 0)
  const reason =
    ADJUST_REASON_MAP[(formData.get("reason") as string) ?? ""] ??
    StockAdjustmentReason.UNSPECIFIED

  try {
    const client = createGrpcClient(AdjustStockService, session.accessToken)
    await client.adjustStock({ productId, quantity, reason })
  } catch (e) {
    redirectIfUnauthenticated(e)
    return toErrorState(e)
  }

  revalidatePath(`/admin/products/${productId}`)
  return { success: true }
}

// ── 画像アップロード ────────────────────────────────────────────────

/**
 * 画像アップロード用の presigned PUT URL と保存用の恒久 URL を発行する。
 * 実ファイルはブラウザが uploadUrl へ直接 PUT する（このアプリ/サーバを経由しない）。
 */
export async function issueImageUploadUrl(
  contentType: string,
): Promise<{ uploadUrl: string; publicUrl: string }> {
  const session = await requireValidSession()
  try {
    const client = createGrpcClient(IssueImageUploadUrlService, session.accessToken)
    const res = await client.issueImageUploadUrl({ contentType })
    return { uploadUrl: res.uploadUrl, publicUrl: res.publicUrl }
  } catch (e) {
    redirectIfUnauthenticated(e)
    throw e
  }
}
