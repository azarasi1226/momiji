"use server"

import { auth, signOut } from "@/auth"
import { redirect } from "next/navigation"
import { revalidatePath } from "next/cache"
import { createGrpcClient } from "@/lib/grpc"
import { FindUserByIdService } from "@/grpc/gen/momiji/user/findbyid/v1/findbyid_pb.js"
import { UpdateUserService } from "@/grpc/gen/momiji/user/update/v1/update_pb.js"
import { DeleteUserService } from "@/grpc/gen/momiji/user/delete/v1/delete_pb.js"
import { RequestEmailChangeService } from "@/grpc/gen/momiji/user/changeemail/request/v1/request_pb.js"
import { ConfirmEmailChangeService } from "@/grpc/gen/momiji/user/changeemail/confirm/v1/confirm_pb.js"
import { ConnectError } from "@connectrpc/connect"
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

export async function fetchProfile(): Promise<UserProfile> {
  const session = await auth()
  if (!session?.accessToken) {
    redirect("/")
  }

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
}

export type UpdateProfileState = {
  success?: boolean
  error?: string
} | null

export async function updateProfile(
  _prevState: UpdateProfileState,
  formData: FormData,
): Promise<UpdateProfileState> {
  const session = await auth()
  if (!session?.accessToken) {
    redirect("/")
  }

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
    if (e instanceof ConnectError) {
      return { error: e.message }
    }
    return { error: "ユーザー情報の更新に失敗しました" }
  }

  revalidatePath("/profile")
  return { success: true }
}

export type DeleteAccountState = {
  error?: string
} | null

export async function deleteAccount(): Promise<DeleteAccountState> {
  const session = await auth()
  if (!session?.accessToken) {
    redirect("/")
  }

  try {
    const client = createGrpcClient(DeleteUserService, session.accessToken)
    await client.deleteUser({})
  } catch (e) {
    if (e instanceof ConnectError) {
      return { error: e.message }
    }
    return { error: "アカウントの削除に失敗しました" }
  }

  await signOut({ redirectTo: "/" })
  return null
}

export type EmailChangeState = {
  success?: boolean
  error?: string
} | null

export async function requestEmailChange(
  _prevState: EmailChangeState,
  formData: FormData,
): Promise<EmailChangeState> {
  const session = await auth()
  if (!session?.accessToken) {
    redirect("/")
  }

  try {
    const client = createGrpcClient(RequestEmailChangeService, session.accessToken)
    await client.requestEmailChange({
      newEmail: formData.get("newEmail") as string,
    })
  } catch (e) {
    if (e instanceof ConnectError) {
      return { error: e.message }
    }
    return { error: "メールアドレス変更リクエストに失敗しました" }
  }

  return { success: true }
}

export async function confirmEmailChange(
  _prevState: EmailChangeState,
  formData: FormData,
): Promise<EmailChangeState> {
  const session = await auth()
  if (!session?.accessToken) {
    redirect("/")
  }

  try {
    const client = createGrpcClient(ConfirmEmailChangeService, session.accessToken)
    await client.confirmEmailChange({
      token: formData.get("token") as string,
    })
  } catch (e) {
    if (e instanceof ConnectError) {
      return { error: e.message }
    }
    return { error: "メールアドレスの変更確認に失敗しました" }
  }

  revalidatePath("/profile")
  return { success: true }
}
