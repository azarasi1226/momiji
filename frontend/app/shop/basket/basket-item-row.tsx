"use client"

/* eslint-disable @next/next/no-img-element */
import { useState, useTransition } from "react"
import { deleteBasketItem, setBasketItem } from "../actions"

type Props = {
  productId: string
  productName: string
  productPrice: number
  productImageUrl: string
  itemQuantity: number
}

/** カゴの1行。 個数変更（絶対値 set）と削除を行う。 */
export function BasketItemRow({
  productId,
  productName,
  productPrice,
  productImageUrl,
  itemQuantity,
}: Props) {
  const [quantity, setQuantity] = useState(itemQuantity)
  const [isPending, startTransition] = useTransition()
  const [error, setError] = useState<string | null>(null)

  const dirty = quantity !== itemQuantity
  const subtotal = productPrice * itemQuantity

  function handleUpdate() {
    setError(null)
    startTransition(async () => {
      const result = await setBasketItem(productId, quantity)
      if (result?.error) setError(result.error)
    })
  }

  function handleRemove() {
    setError(null)
    startTransition(async () => {
      const result = await deleteBasketItem(productId)
      if (result?.error) setError(result.error)
    })
  }

  return (
    <div className="flex flex-col gap-2 border-b border-zinc-100 py-4 dark:border-zinc-800">
      <div className="flex items-center gap-4">
        <div className="flex h-16 w-16 shrink-0 items-center justify-center overflow-hidden rounded-lg bg-zinc-100 dark:bg-zinc-900">
          {productImageUrl ? (
            <img src={productImageUrl} alt={productName} className="h-full w-full object-cover" />
          ) : (
            <span className="text-[10px] text-zinc-400 dark:text-zinc-600">画像なし</span>
          )}
        </div>

        <div className="min-w-0 flex-1">
          <p className="truncate text-sm font-medium text-black dark:text-zinc-50">{productName}</p>
          <p className="text-xs text-zinc-500 dark:text-zinc-400">
            単価 ¥{productPrice.toLocaleString("ja-JP")}
          </p>
        </div>

        <div className="flex items-center gap-2">
          <select
            aria-label="個数"
            value={quantity}
            onChange={(e) => setQuantity(Number(e.target.value))}
            disabled={isPending}
            className="h-9 rounded-lg border border-zinc-200 px-2 text-sm dark:border-zinc-700 dark:bg-zinc-900 dark:text-zinc-50"
          >
            {Array.from({ length: 99 }, (_, i) => i + 1).map((n) => (
              <option key={n} value={n}>
                {n}
              </option>
            ))}
          </select>
          <button
            type="button"
            onClick={handleUpdate}
            disabled={isPending || !dirty}
            className="h-9 rounded-full border border-zinc-200 px-3 text-xs text-zinc-700 transition-colors hover:bg-zinc-100 disabled:opacity-40 dark:border-zinc-700 dark:text-zinc-200 dark:hover:bg-zinc-900"
          >
            更新
          </button>
        </div>

        <p className="w-24 text-right text-sm font-semibold text-black dark:text-zinc-50">
          ¥{subtotal.toLocaleString("ja-JP")}
        </p>

        <button
          type="button"
          onClick={handleRemove}
          disabled={isPending}
          className="text-xs text-red-600 transition-colors hover:underline disabled:opacity-40 dark:text-red-400"
        >
          削除
        </button>
      </div>
      {error && <p className="text-xs text-red-600 dark:text-red-400">{error}</p>}
    </div>
  )
}
