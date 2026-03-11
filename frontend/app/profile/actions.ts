"use server"

import { auth, BACKEND_URL } from "@/auth"
import { redirect } from "next/navigation"
import { revalidatePath } from "next/cache"

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
    const errorBody = await res.text()
    console.error("UpdateUser API failed:", res.status, errorBody)
    return { error: `ユーザー情報の更新に失敗しました (${errorBody})` }
  }

  revalidatePath("/profile")
  return { success: true }
}
