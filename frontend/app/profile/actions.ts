"use server"

import { auth, signOut, BACKEND_URL } from "@/auth"
import { redirect } from "next/navigation"
import { revalidatePath } from "next/cache"

async function extractErrorMessage(res: Response, fallback: string): Promise<string> {
  try {
    const body = await res.json()
    return body.error ?? fallback
  } catch {
    return fallback
  }
}

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

  const res = await fetch(`${BACKEND_URL}/users/me`, {
    headers: {
      Authorization: `Bearer ${session.accessToken}`,
    },
    cache: "no-store",
  })

  if (!res.ok) {
    throw new Error("ユーザー情報の取得に失敗しました")
  }

  return res.json()
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

  const body = {
    name: formData.get("name") as string,
    phoneNumber: formData.get("phoneNumber") as string,
    postalCode: formData.get("postalCode") as string,
    address1: formData.get("address1") as string,
    address2: formData.get("address2") as string,
  }

  const res = await fetch(`${BACKEND_URL}/users/me`, {
    method: "PUT",
    headers: {
      Authorization: `Bearer ${session.accessToken}`,
      "Content-Type": "application/json",
    },
    body: JSON.stringify(body),
  })

  if (!res.ok) {
    const message = await extractErrorMessage(res, "ユーザー情報の更新に失敗しました")
    return { error: message }
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

  const res = await fetch(`${BACKEND_URL}/users/me`, {
    method: "DELETE",
    headers: {
      Authorization: `Bearer ${session.accessToken}`,
    },
  })

  if (!res.ok) {
    const message = await extractErrorMessage(res, "アカウントの削除に失敗しました")
    return { error: message }
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

  const newEmail = formData.get("newEmail") as string

  const res = await fetch(`${BACKEND_URL}/users/me/email/change-request`, {
    method: "POST",
    headers: {
      Authorization: `Bearer ${session.accessToken}`,
      "Content-Type": "application/json",
    },
    body: JSON.stringify({ newEmail }),
  })

  if (!res.ok) {
    const message = await extractErrorMessage(res, "メールアドレス変更リクエストに失敗しました")
    return { error: message }
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

  const token = formData.get("token") as string

  const res = await fetch(`${BACKEND_URL}/users/me/email/change-confirm`, {
    method: "POST",
    headers: {
      Authorization: `Bearer ${session.accessToken}`,
      "Content-Type": "application/json",
    },
    body: JSON.stringify({ token }),
  })

  if (!res.ok) {
    const message = await extractErrorMessage(res, "メールアドレスの変更確認に失敗しました")
    return { error: message }
  }

  revalidatePath("/profile")
  return { success: true }
}