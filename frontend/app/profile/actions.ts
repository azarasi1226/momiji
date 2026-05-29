"use server"

import { signOut } from "@/auth"
import { redirect } from "next/navigation"
import { revalidatePath } from "next/cache"
import { createGrpcClient } from "@/lib/grpc"
import { requireValidSession } from "@/lib/session"
import { FindUserByIdService } from "@/grpc/gen/momiji/user/findbyid/v1/findbyid_pb.js"
import { UpdateUserService } from "@/grpc/gen/momiji/user/update/v1/update_pb.js"
import { DeleteUserService } from "@/grpc/gen/momiji/user/delete/v1/delete_pb.js"
import { RequestEmailChangeService } from "@/grpc/gen/momiji/user/changeemail/request/v1/request_pb.js"
import { ConfirmEmailChangeService } from "@/grpc/gen/momiji/user/changeemail/confirm/v1/confirm_pb.js"
import { ErrorDetailSchema } from "@/grpc/gen/momiji/common/v1/error_pb.js"
import { Code, ConnectError } from "@connectrpc/connect"
import { timestampDate } from "@bufbuild/protobuf/wkt"

export type UserProfile = {
  id: string
  email: string
  name: string
  phoneNumber: string
  postalCode: string
  address1: string
  address2: string
  createdAt: string
  updatedAt: string
}

/**
 * 構造化エラーを backend から取り出す共通パーサ。
 *
 * backend は ErrorDetail を gRPC Status.details に乗せて返す:
 * - useCaseError: ビジネスルール違反 (例: ユーザー未存在)
 * - validationError: 値オブジェクト validation の集約エラー
 *
 * 返り値:
 * - useCaseError: 表示用文字列
 * - fieldErrors: field 名 → message のマップ (form の field 別ハイライト用)
 * - fallback: details が無い場合の生メッセージ
 */
function parseConnectError(e: unknown): {
  useCaseError?: string
  fieldErrors?: Record<string, string>
  fallback?: string
} | null {
  if (!(e instanceof ConnectError)) return null
  const details = e.findDetails(ErrorDetailSchema)
  if (details.length === 0) return { fallback: e.message }
  const detail = details[0]
  if (detail.error.case === "useCaseError") {
    return { useCaseError: detail.error.value.message }
  }
  if (detail.error.case === "validationError") {
    const fieldErrors: Record<string, string> = {}
    for (const fe of detail.error.value.errors) {
      fieldErrors[fe.fieldName] = fe.message
    }
    return { fieldErrors }
  }
  return { fallback: e.message }
}

/**
 * gRPC 呼び出し中に backend から UNAUTHENTICATED が返った場合の共通ハンドラ。
 * Server Action / Server Component から呼んで、 "/" に飛ばして再ログインを促す。
 */
function redirectIfUnauthenticated(e: unknown): never | void {
  if (e instanceof ConnectError && e.code === Code.Unauthenticated) {
    redirect("/")
  }
}

export async function fetchProfile(): Promise<UserProfile> {
  const session = await requireValidSession()

  try {
    const client = createGrpcClient(FindUserByIdService, session.accessToken)
    const response = await client.findUserById({})

    return {
      id: response.id,
      email: response.email,
      name: response.name,
      phoneNumber: response.phoneNumber,
      postalCode: response.postalCode,
      address1: response.address1,
      address2: response.address2,
      createdAt: response.createdAt ? timestampDate(response.createdAt).toISOString() : "",
      updatedAt: response.updatedAt ? timestampDate(response.updatedAt).toISOString() : "",
    }
  } catch (e) {
    redirectIfUnauthenticated(e)
    throw e
  }
}

export type UpdateProfileState = {
  success?: boolean
  error?: string
  fieldErrors?: Record<string, string>
} | null

export async function updateProfile(
  _prevState: UpdateProfileState,
  formData: FormData,
): Promise<UpdateProfileState> {
  const session = await requireValidSession()

  try {
    const client = createGrpcClient(UpdateUserService, session.accessToken)
    await client.updateUser({
      name: formData.get("name") as string,
      phoneNumber: formData.get("phoneNumber") as string,
      postalCode: formData.get("postalCode") as string,
      address1: formData.get("address1") as string,
      address2: formData.get("address2") as string,
    })
  } catch (e) {
    redirectIfUnauthenticated(e)
    const parsed = parseConnectError(e)
    if (parsed?.fieldErrors) return { fieldErrors: parsed.fieldErrors }
    if (parsed?.useCaseError) return { error: parsed.useCaseError }
    if (parsed?.fallback) return { error: parsed.fallback }
    return { error: "ユーザー情報の更新に失敗しました" }
  }

  revalidatePath("/profile")
  return { success: true }
}

export type DeleteAccountState = {
  error?: string
} | null

export async function deleteAccount(): Promise<DeleteAccountState> {
  const session = await requireValidSession()

  try {
    const client = createGrpcClient(DeleteUserService, session.accessToken)
    await client.deleteUser({})
  } catch (e) {
    redirectIfUnauthenticated(e)
    const parsed = parseConnectError(e)
    if (parsed?.useCaseError) return { error: parsed.useCaseError }
    if (parsed?.fallback) return { error: parsed.fallback }
    return { error: "アカウントの削除に失敗しました" }
  }

  await signOut({ redirectTo: "/" })
  return null
}

export type EmailChangeState = {
  success?: boolean
  error?: string
  fieldErrors?: Record<string, string>
} | null

export async function requestEmailChange(
  _prevState: EmailChangeState,
  formData: FormData,
): Promise<EmailChangeState> {
  const session = await requireValidSession()

  try {
    const client = createGrpcClient(RequestEmailChangeService, session.accessToken)
    await client.requestEmailChange({
      newEmail: formData.get("newEmail") as string,
    })
  } catch (e) {
    redirectIfUnauthenticated(e)
    const parsed = parseConnectError(e)
    if (parsed?.fieldErrors) return { fieldErrors: parsed.fieldErrors }
    if (parsed?.useCaseError) return { error: parsed.useCaseError }
    if (parsed?.fallback) return { error: parsed.fallback }
    return { error: "メールアドレス変更リクエストに失敗しました" }
  }

  return { success: true }
}

export async function confirmEmailChange(
  _prevState: EmailChangeState,
  formData: FormData,
): Promise<EmailChangeState> {
  const session = await requireValidSession()

  try {
    const client = createGrpcClient(ConfirmEmailChangeService, session.accessToken)
    await client.confirmEmailChange({
      token: formData.get("token") as string,
    })
  } catch (e) {
    redirectIfUnauthenticated(e)
    const parsed = parseConnectError(e)
    if (parsed?.fieldErrors) return { fieldErrors: parsed.fieldErrors }
    if (parsed?.useCaseError) return { error: parsed.useCaseError }
    if (parsed?.fallback) return { error: parsed.fallback }
    return { error: "メールアドレスの変更確認に失敗しました" }
  }

  revalidatePath("/profile")
  return { success: true }
}
