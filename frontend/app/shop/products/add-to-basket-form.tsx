"use client"

import { useState, useTransition } from "react"
import { setBasketItem } from "../actions"

/**
 * 商品カードの「カゴに入れる」。 個数（1〜99）を選んで set する。
 *
 * 個数は**絶対値セット**（加算ではない）なので、 既にカゴにある商品なら指定個数で上書きされる。
 * UI 上はその旨を「カゴに入れる」で表現し、 細かな増減はカゴ画面で行う方針。
 */
export function AddToBasketForm({ productId }: { productId: string }) {
  const [quantity, setQuantity] = useState(1)
  const [isPending, startTransition] = useTransition()
  const [message, setMessage] = useState<{ ok: boolean; text: string } | null>(null)

  function handleAdd() {
    setMessage(null)
    startTransition(async () => {
      const result = await setBasketItem(productId, quantity)
      if (result?.error) {
        setMessage({ ok: false, text: result.error })
      } else {
        setMessage({ ok: true, text: "カゴに入れました" })
      }
    })
  }

  return (
    <div className="flex flex-col gap-2">
      <div className="flex items-center gap-2">
        <label htmlFor={`qty-${productId}`} className="sr-only">
          個数
        </label>
        <select
          id={`qty-${productId}`}
          value={quantity}
          onChange={(e) => setQuantity(Number(e.target.value))}
          disabled={isPending}
          className="h-10 rounded-lg border border-zinc-200 px-2 text-sm dark:border-zinc-700 dark:bg-zinc-900 dark:text-zinc-50"
        >
          {Array.from({ length: 99 }, (_, i) => i + 1).map((n) => (
            <option key={n} value={n}>
              {n}
            </option>
          ))}
        </select>
        <button
          type="button"
          onClick={handleAdd}
          disabled={isPending}
          className="flex h-10 flex-1 items-center justify-center rounded-full bg-foreground px-4 text-sm text-background transition-colors hover:bg-[#383838] disabled:opacity-50 dark:hover:bg-[#ccc]"
        >
          {isPending ? "追加中..." : "カゴに入れる"}
        </button>
      </div>
      {message && (
        <p
          className={
            message.ok
              ? "text-xs text-green-600 dark:text-green-400"
              : "text-xs text-red-600 dark:text-red-400"
          }
        >
          {message.text}
        </p>
      )}
    </div>
  )
}
