"use client"

import { useActionState } from "react"
import { updateProfile, type UpdateProfileState, type UserProfile } from "./actions"

export function ProfileForm({ profile }: { profile: UserProfile }) {
  const [state, formAction, isPending] = useActionState<UpdateProfileState, FormData>(
    updateProfile,
    null,
  )

  return (
    <form action={formAction} className="flex w-full flex-col gap-4">
      <div className="flex flex-col gap-1">
        <label htmlFor="email" className="text-sm text-zinc-500 dark:text-zinc-400">
          メールアドレス
        </label>
        <input
          id="email"
          type="email"
          value={profile.email}
          disabled
          className="rounded-lg border border-zinc-200 bg-zinc-100 px-4 py-2 text-zinc-500 dark:border-zinc-700 dark:bg-zinc-800 dark:text-zinc-400"
        />
      </div>

      <div className="flex flex-col gap-1">
        <label htmlFor="name" className="text-sm text-zinc-500 dark:text-zinc-400">
          名前
        </label>
        <input
          id="name"
          name="name"
          type="text"
          defaultValue={profile.name}
          required
          className="rounded-lg border border-zinc-200 px-4 py-2 dark:border-zinc-700 dark:bg-zinc-900 dark:text-zinc-50"
        />
      </div>

      <div className="flex flex-col gap-1">
        <label htmlFor="phoneNumber" className="text-sm text-zinc-500 dark:text-zinc-400">
          電話番号
        </label>
        <input
          id="phoneNumber"
          name="phoneNumber"
          type="tel"
          defaultValue={profile.phoneNumber}
          required
          className="rounded-lg border border-zinc-200 px-4 py-2 dark:border-zinc-700 dark:bg-zinc-900 dark:text-zinc-50"
        />
      </div>

      <div className="flex flex-col gap-1">
        <label htmlFor="postalCode" className="text-sm text-zinc-500 dark:text-zinc-400">
          郵便番号
        </label>
        <input
          id="postalCode"
          name="postalCode"
          type="text"
          defaultValue={profile.postalCode}
          required
          className="rounded-lg border border-zinc-200 px-4 py-2 dark:border-zinc-700 dark:bg-zinc-900 dark:text-zinc-50"
        />
      </div>

      <div className="flex flex-col gap-1">
        <label htmlFor="address1" className="text-sm text-zinc-500 dark:text-zinc-400">
          住所1
        </label>
        <input
          id="address1"
          name="address1"
          type="text"
          defaultValue={profile.address1}
          required
          className="rounded-lg border border-zinc-200 px-4 py-2 dark:border-zinc-700 dark:bg-zinc-900 dark:text-zinc-50"
        />
      </div>

      <div className="flex flex-col gap-1">
        <label htmlFor="address2" className="text-sm text-zinc-500 dark:text-zinc-400">
          住所2
        </label>
        <input
          id="address2"
          name="address2"
          type="text"
          defaultValue={profile.address2}
          required
          className="rounded-lg border border-zinc-200 px-4 py-2 dark:border-zinc-700 dark:bg-zinc-900 dark:text-zinc-50"
        />
      </div>

      {state?.error && (
        <p className="text-sm text-red-500">{state.error}</p>
      )}
      {state?.success && (
        <p className="text-sm text-green-600 dark:text-green-400">更新しました</p>
      )}

      <button
        type="submit"
        disabled={isPending}
        className="mt-2 flex h-12 items-center justify-center rounded-full bg-foreground px-8 text-background transition-colors hover:bg-[#383838] disabled:opacity-50 dark:hover:bg-[#ccc]"
      >
        {isPending ? "更新中..." : "更新"}
      </button>
    </form>
  )
}
