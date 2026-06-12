"use server"

import { timestampDate } from "@bufbuild/protobuf/wkt"
import { revalidatePath } from "next/cache"
import { redirect } from "next/navigation"
import { ulid } from "ulid"
import { createGrpcClient } from "@/lib/grpc"
import { requireValidSession } from "@/lib/session"
import { redirectIfUnauthenticated, parseConnectError } from "@/lib/grpc-error"
import { ListBrandsService } from "@/grpc/gen/momiji/brand/list/v1/list_pb.js"
import { FindBrandByIdService } from "@/grpc/gen/momiji/brand/findbyid/v1/findbyid_pb.js"
import { CreateBrandService } from "@/grpc/gen/momiji/brand/create/v1/create_pb.js"
import { UpdateBrandService } from "@/grpc/gen/momiji/brand/update/v1/update_pb.js"
import { ArchiveBrandService } from "@/grpc/gen/momiji/brand/archive/v1/archive_pb.js"
import { BrandStatus } from "@/grpc/gen/momiji/brand/v1/status_pb.js"

export type Brand = {
  id: string
  name: string
  description: string
  status: string
  createdAt: string
  updatedAt: string
}

/** proto enum (BrandStatus) を正準コード文字列に変換する（表示用ラベルは lib/status-labels.ts）。 */
function brandStatusName(status: BrandStatus): string {
  switch (status) {
    case BrandStatus.ACTIVE:
      return "ACTIVE"
    case BrandStatus.ARCHIVED:
      return "ARCHIVED"
    default:
      return "UNKNOWN"
  }
}

export async function listBrands(): Promise<Brand[]> {
  const session = await requireValidSession()
  try {
    const client = createGrpcClient(ListBrandsService, session.accessToken)
    const res = await client.listBrands({})
    return res.brands.map((b) => ({
      id: b.id,
      name: b.name,
      description: b.description,
      status: brandStatusName(b.status),
      createdAt: b.createdAt ? timestampDate(b.createdAt).toISOString() : "",
      updatedAt: b.updatedAt ? timestampDate(b.updatedAt).toISOString() : "",
    }))
  } catch (e) {
    redirectIfUnauthenticated(e)
    throw e
  }
}

export async function fetchBrand(id: string): Promise<Brand> {
  const session = await requireValidSession()
  try {
    const client = createGrpcClient(FindBrandByIdService, session.accessToken)
    const res = await client.findBrandById({ id })
    return {
      id: res.id,
      name: res.name,
      description: res.description,
      status: brandStatusName(res.status),
      createdAt: res.createdAt ? timestampDate(res.createdAt).toISOString() : "",
      updatedAt: res.updatedAt ? timestampDate(res.updatedAt).toISOString() : "",
    }
  } catch (e) {
    redirectIfUnauthenticated(e)
    throw e
  }
}

export type BrandFormState = {
  success?: boolean
  error?: string
  fieldErrors?: Record<string, string>
} | null

function toErrorState(e: unknown): BrandFormState {
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

export async function createBrand(
  _prevState: BrandFormState,
  formData: FormData,
): Promise<BrandFormState> {
  const session = await requireValidSession()
  const name = (formData.get("name") as string) ?? ""
  const description = (formData.get("description") as string) ?? ""

  try {
    const client = createGrpcClient(CreateBrandService, session.accessToken)
    // id は BFF 採番（冪等キー）。 ここで ULID を生成して渡す。
    await client.createBrand({ id: ulid(), name, description })
  } catch (e) {
    redirectIfUnauthenticated(e)
    return toErrorState(e)
  }

  revalidatePath("/admin/brands")
  redirect("/admin/brands")
}

export async function updateBrand(
  _prevState: BrandFormState,
  formData: FormData,
): Promise<BrandFormState> {
  const session = await requireValidSession()
  const id = formData.get("id") as string
  const name = (formData.get("name") as string) ?? ""
  const description = (formData.get("description") as string) ?? ""

  try {
    const client = createGrpcClient(UpdateBrandService, session.accessToken)
    await client.updateBrand({ id, name, description })
  } catch (e) {
    redirectIfUnauthenticated(e)
    return toErrorState(e)
  }

  revalidatePath("/admin/brands")
  revalidatePath(`/admin/brands/${id}`)
  return { success: true }
}

export async function archiveBrand(id: string): Promise<void> {
  const session = await requireValidSession()
  try {
    const client = createGrpcClient(ArchiveBrandService, session.accessToken)
    await client.archiveBrand({ id })
  } catch (e) {
    redirectIfUnauthenticated(e)
    throw e
  }

  revalidatePath("/admin/brands")
  redirect("/admin/brands")
}
