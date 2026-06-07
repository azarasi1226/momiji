"use client"

import { useActionState } from "react"
import { updateBrand, type Brand, type BrandFormState } from "../actions"

function inputClassName(hasError: boolean): string {
  const base =
    "rounded-lg border px-4 py-2 dark:bg-zinc-900 dark:text-zinc-50"
  return hasError
    ? `${base} border-red-500 dark:border-red-400`
    : `${base} border-zinc-200 dark:border-zinc-700`
}

function FieldErrorMessage({ message }: { message?: string }) {
  if (!message) return null
  return <p className="text-xs text-red-500 dark:text-red-400">{message}</p>
}

export function BrandEditForm({ brand }: { brand: Brand }) {
  const [state, formAction, isPending] = useActionState<BrandFormState, FormData>(
    updateBrand,
    null,
  )
  const fieldErrors = state?.fieldErrors

  return (
    <form action={formAction} className="flex w-full flex-col gap-4">
      <input type="hidden" name="id" value={brand.id} />

      <div className="flex flex-col gap-1">
        <label htmlFor="name" className="text-sm text-zinc-500 dark:text-zinc-400">
          ブランド名
        </label>
        <input
          id="name"
          name="name"
          type="text"
          defaultValue={brand.name}
          required
          className={inputClassName(!!fieldErrors?.name)}
        />
        <FieldErrorMessage message={fieldErrors?.name} />
      </div>

      <div className="flex flex-col gap-1">
        <label
          htmlFor="description"
          className="text-sm text-zinc-500 dark:text-zinc-400"
        >
          説明（任意）
        </label>
        <textarea
          id="description"
          name="description"
          rows={5}
          defaultValue={brand.description}
          className={inputClassName(!!fieldErrors?.description)}
        />
        <FieldErrorMessage message={fieldErrors?.description} />
      </div>

      {state?.error && <p className="text-sm text-red-500">{state.error}</p>}
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
