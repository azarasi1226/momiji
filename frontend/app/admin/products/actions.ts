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

export async function listProducts(): Promise<Product[]> {
  const session = await requireValidSession()
  try {
    const client = createGrpcClient(ListProductsService, session.accessToken)
    const res = await client.listProducts({})
    return res.products.map((p) => ({
      id: p.id,
      brandId: p.brandId,
      name: p.name,
      description: p.description,
      imageUrl: p.imageUrl ?? "",
      price: p.price,
      status: productStatusName(p.status),
      createdAt: p.createdAt ? timestampDate(p.createdAt).toISOString() : "",
      updatedAt: p.updatedAt ? timestampDate(p.updatedAt).toISOString() : "",
    }))
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

/** 商品名解決用に brandId → ブランド名 のマップを作る（一覧の表示で生 ULID を出さないため）。 */
export async function brandNameMap(): Promise<Record<string, string>> {
  const brands = await listBrands()
  return Object.fromEntries(brands.map((b) => [b.id, b.name]))
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
    // id は BFF 採番（冪等キー）。 image_url は proto3 optional なので空なら未設定で送る。
    await client.createProduct({
      id: ulid(),
      brandId,
      name,
      description,
      imageUrl: imageUrl || undefined,
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
      imageUrl: imageUrl || undefined,
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
