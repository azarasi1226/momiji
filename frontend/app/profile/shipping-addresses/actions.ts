"use server"

import { revalidatePath } from "next/cache"
import { ulid } from "ulid"
import { createGrpcClient } from "@/lib/grpc"
import { joinPhoneNumber, joinPostalCode } from "@/lib/form-segments"
import { requireValidSession } from "@/lib/session"
import { redirectIfUnauthenticated, parseConnectError } from "@/lib/grpc-error"
import { RegisterShippingAddressService } from "@/grpc/gen/momiji/user/shippingaddress/register/v1/register_pb.js"
import { UpdateShippingAddressService } from "@/grpc/gen/momiji/user/shippingaddress/update/v1/update_pb.js"
import { DeleteShippingAddressService } from "@/grpc/gen/momiji/user/shippingaddress/delete/v1/delete_pb.js"
import { ChangeDefaultShippingAddressService } from "@/grpc/gen/momiji/user/shippingaddress/changedefault/v1/changedefault_pb.js"
import { ListShippingAddressesService } from "@/grpc/gen/momiji/user/shippingaddress/list/v1/list_pb.js"

export type ShippingAddress = {
  id: string
  name: string
  phoneNumber: string
  postalCode: string
  prefecture: string
  city: string
  streetAddress: string
  building: string
  deliveryNote: string
  isDefault: boolean
}

export type SaveAddressState = {
  success?: boolean
  id?: string
  error?: string
  fieldErrors?: Record<string, string>
} | null

function toSaveErrorState(e: unknown, fallback: string): SaveAddressState {
  const parsed = parseConnectError(e)
  if (parsed?.fieldErrors) return { fieldErrors: parsed.fieldErrors }
  if (parsed?.businessError) return { error: parsed.businessError }
  if (parsed?.unknownError) {
    return { error: `${parsed.unknownError.message} (問い合わせ番号: ${parsed.unknownError.correlationId})` }
  }
  return { error: fallback }
}

/** 配送先一覧を取得する（登録順）。 */
export async function fetchShippingAddresses(): Promise<ShippingAddress[]> {
  const session = await requireValidSession()
  try {
    const client = createGrpcClient(ListShippingAddressesService, session.accessToken)
    const response = await client.listShippingAddresses({})
    return response.shippingAddresses.map((address) => ({
      id: address.id,
      name: address.name,
      phoneNumber: address.phoneNumber,
      postalCode: address.postalCode,
      prefecture: address.prefecture,
      city: address.city,
      streetAddress: address.streetAddress,
      building: address.building,
      deliveryNote: address.deliveryNote,
      isDefault: address.isDefault,
    }))
  } catch (e) {
    redirectIfUnauthenticated(e)
    throw e
  }
}

/** 配送先を登録する。 id は BFF 採番（冪等キー）。 成功時は採番した id を返す。 */
export async function registerShippingAddress(formData: FormData): Promise<SaveAddressState> {
  const session = await requireValidSession()
  const id = ulid()
  try {
    const client = createGrpcClient(RegisterShippingAddressService, session.accessToken)
    await client.registerShippingAddress({
      id,
      name: formData.get("name") as string,
      phoneNumber: joinPhoneNumber(formData),
      postalCode: joinPostalCode(formData),
      prefecture: formData.get("prefecture") as string,
      city: formData.get("city") as string,
      streetAddress: formData.get("streetAddress") as string,
      building: (formData.get("building") as string) ?? "",
      deliveryNote: (formData.get("deliveryNote") as string) ?? "",
      makeDefault: formData.get("makeDefault") === "on",
    })
  } catch (e) {
    redirectIfUnauthenticated(e)
    return toSaveErrorState(e, "配送先の登録に失敗しました")
  }
  revalidatePath("/profile/shipping-addresses")
  return { success: true, id }
}

/** 配送先を編集する。 */
export async function updateShippingAddress(
  shippingAddressId: string,
  formData: FormData,
): Promise<SaveAddressState> {
  const session = await requireValidSession()
  try {
    const client = createGrpcClient(UpdateShippingAddressService, session.accessToken)
    await client.updateShippingAddress({
      shippingAddressId,
      name: formData.get("name") as string,
      phoneNumber: joinPhoneNumber(formData),
      postalCode: joinPostalCode(formData),
      prefecture: formData.get("prefecture") as string,
      city: formData.get("city") as string,
      streetAddress: formData.get("streetAddress") as string,
      building: (formData.get("building") as string) ?? "",
      deliveryNote: (formData.get("deliveryNote") as string) ?? "",
    })
  } catch (e) {
    redirectIfUnauthenticated(e)
    return toSaveErrorState(e, "配送先の編集に失敗しました")
  }
  revalidatePath("/profile/shipping-addresses")
  return { success: true, id: shippingAddressId }
}

export type AddressActionState = { error?: string } | null

/** 配送先を削除する（default 削除時の昇格は backend が行う）。 */
export async function deleteShippingAddress(shippingAddressId: string): Promise<AddressActionState> {
  const session = await requireValidSession()
  try {
    const client = createGrpcClient(DeleteShippingAddressService, session.accessToken)
    await client.deleteShippingAddress({ shippingAddressId })
  } catch (e) {
    redirectIfUnauthenticated(e)
    const parsed = parseConnectError(e)
    if (parsed?.businessError) return { error: parsed.businessError }
    return { error: "配送先の削除に失敗しました" }
  }
  revalidatePath("/profile/shipping-addresses")
  return null
}

/** デフォルト配送先を変更する。 */
export async function changeDefaultShippingAddress(shippingAddressId: string): Promise<AddressActionState> {
  const session = await requireValidSession()
  try {
    const client = createGrpcClient(ChangeDefaultShippingAddressService, session.accessToken)
    await client.changeDefaultShippingAddress({ shippingAddressId })
  } catch (e) {
    redirectIfUnauthenticated(e)
    const parsed = parseConnectError(e)
    if (parsed?.businessError) return { error: parsed.businessError }
    return { error: "デフォルト配送先の変更に失敗しました" }
  }
  revalidatePath("/profile/shipping-addresses")
  return null
}

export type AddressLookupResult = {
  prefecture: string
  city: string
  town: string
} | null

/**
 * 郵便番号から住所を引く（zipcloud: 日本郵便データの無料 API・キー不要）。
 * BFF サーバー側で fetch する（CORS 回避＋将来の API 差し替え容易化）。
 *
 * 実レスポンス形式（実 API で確認済み）:
 *   ヒット:   { results: [{ address1: 都道府県, address2: 市区町村, address3: 町域 }], status: 200 }
 *   未ヒット: { results: null, status: 200 }
 *
 * 失敗・未ヒットは null を返し、 呼び出し側は無言で手入力にフォールバックする（必須経路にしない）。
 */
export async function lookupAddress(postalCode: string): Promise<AddressLookupResult> {
  if (!/^\d{7}$/.test(postalCode)) return null
  try {
    const res = await fetch(`https://zipcloud.ibsnet.co.jp/api/search?zipcode=${postalCode}`, {
      // 郵便番号データは滅多に変わらないのでキャッシュしてよい
      cache: "force-cache",
    })
    if (!res.ok) return null
    const data = (await res.json()) as {
      results: { address1?: string; address2?: string; address3?: string }[] | null
    }
    const result = data.results?.[0]
    if (!result) return null
    return {
      prefecture: result.address1 ?? "",
      city: result.address2 ?? "",
      town: result.address3 ?? "",
    }
  } catch {
    return null
  }
}
