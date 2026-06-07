"use client"

import { useActionState } from "react"
import { createProduct, type ProductFormState } from "../actions"

function inputClassName(hasError: boolean): string {
  const base = "rounded-lg border px-4 py-2 dark:bg-zinc-900 dark:text-zinc-50"
  return hasError
    ? `${base} border-red-500 dark:border-red-400`
    : `${base} border-zinc-200 dark:border-zinc-700`
}

function FieldErrorMessage({ message }: { message?: string }) {
  if (!message) return null
  return <p className="text-xs text-red-500 dark:text-red-400">{message}</p>
}

export function ProductCreateForm({
  brands,
}: {
  brands: { id: string; name: string }[]
}) {
  const [state, formAction, isPending] = useActionState<
    ProductFormState,
    FormData
  >(createProduct, null)
  const fieldErrors = state?.fieldErrors

  return (
    <form action={formAction} className="flex w-full flex-col gap-4">
      <div className="flex flex-col gap-1">
        <label
          htmlFor="brandId"
          className="text-sm text-zinc-500 dark:text-zinc-400"
        >
          ブランド
        </label>
        {brands.length === 0 ? (
          <p className="text-sm text-red-500 dark:text-red-400">
            紐づけられる ACTIVE なブランドがありません。 先にブランドを作成してください。
          </p>
        ) : (
          <select
            id="brandId"
            name="brandId"
            required
            defaultValue=""
            className={inputClassName(!!fieldErrors?.id)}
          >
            <option value="" disabled>
              選択してください
            </option>
            {brands.map((brand) => (
              <option key={brand.id} value={brand.id}>
                {brand.name}
              </option>
            ))}
          </select>
        )}
        <FieldErrorMessage message={fieldErrors?.id} />
      </div>

      <div className="flex flex-col gap-1">
        <label htmlFor="name" className="text-sm text-zinc-500 dark:text-zinc-400">
          商品名
        </label>
        <input
          id="name"
          name="name"
          type="text"
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
          商品説明
        </label>
        <textarea
          id="description"
          name="description"
          rows={5}
          required
          className={inputClassName(!!fieldErrors?.description)}
        />
        <FieldErrorMessage message={fieldErrors?.description} />
      </div>

      <div className="flex flex-col gap-1">
        <label
          htmlFor="imageUrl"
          className="text-sm text-zinc-500 dark:text-zinc-400"
        >
          画像URL（任意）
        </label>
        <input
          id="imageUrl"
          name="imageUrl"
          type="url"
          className={inputClassName(!!fieldErrors?.imageUrl)}
        />
        <FieldErrorMessage message={fieldErrors?.imageUrl} />
      </div>

      <div className="flex flex-col gap-1">
        <label
          htmlFor="price"
          className="text-sm text-zinc-500 dark:text-zinc-400"
        >
          価格（円）
        </label>
        <input
          id="price"
          name="price"
          type="number"
          min={1}
          required
          className={inputClassName(!!fieldErrors?.price)}
        />
        <FieldErrorMessage message={fieldErrors?.price} />
      </div>

      {state?.error && <p className="text-sm text-red-500">{state.error}</p>}

      <button
        type="submit"
        disabled={isPending || brands.length === 0}
        className="mt-2 flex h-12 items-center justify-center rounded-full bg-foreground px-8 text-background transition-colors hover:bg-[#383838] disabled:opacity-50 dark:hover:bg-[#ccc]"
      >
        {isPending ? "作成中..." : "作成"}
      </button>
    </form>
  )
}
