"use client"

import Link from "next/link"
import { useState, useTransition } from "react"
import { setBasketItem } from "../../actions"

// 在庫が少ないとき「残りN点」を出す閾値（Amazon 風の煽り表示）。
const LOW_STOCK_THRESHOLD = 10

export function AddToBasketPanel({
  productId,
  price,
  available,
}: {
  productId: string
  price: number
  available: number
}) {
  const inStock = available > 0
  const maxQuantity = Math.min(99, available)

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
        setMessage({ ok: true, text: `カートに入れました（${quantity}点）` })
      }
    })
  }

  return (
    <div className="flex w-full flex-col gap-4 rounded-2xl border border-zinc-200 p-5 dark:border-zinc-800">
      <p className="text-2xl font-bold text-black dark:text-zinc-50">
        ¥{price.toLocaleString("ja-JP")}
        <span className="ml-1 text-sm font-normal text-zinc-500 dark:text-zinc-400">税込</span>
      </p>

      {/* 在庫状況 */}
      <StockStatus available={available} />

      {inStock ? (
        <>
          <div className="flex items-center gap-2">
            <label htmlFor="quantity" className="text-sm text-zinc-600 dark:text-zinc-400">
              数量
            </label>
            <select
              id="quantity"
              value={quantity}
              onChange={(e) => setQuantity(Number(e.target.value))}
              disabled={isPending}
              className="h-10 rounded-lg border border-zinc-200 px-3 text-sm dark:border-zinc-700 dark:bg-zinc-900 dark:text-zinc-50"
            >
              {Array.from({ length: maxQuantity }, (_, i) => i + 1).map((n) => (
                <option key={n} value={n}>
                  {n}
                </option>
              ))}
            </select>
          </div>

          <button
            type="button"
            onClick={handleAdd}
            disabled={isPending}
            className="flex h-12 items-center justify-center rounded-full bg-foreground px-6 text-sm font-medium text-background transition-colors hover:bg-[#383838] disabled:opacity-50 dark:hover:bg-[#ccc]"
          >
            {isPending ? "追加中..." : "カートに入れる"}
          </button>
        </>
      ) : (
        <button
          type="button"
          disabled
          className="flex h-12 cursor-not-allowed items-center justify-center rounded-full bg-zinc-200 px-6 text-sm font-medium text-zinc-500 dark:bg-zinc-800 dark:text-zinc-500"
        >
          在庫切れ
        </button>
      )}

      {message && (
        <div className="flex flex-col gap-1">
          <p
            className={
              message.ok
                ? "text-xs text-green-600 dark:text-green-400"
                : "text-xs text-red-600 dark:text-red-400"
            }
          >
            {message.text}
          </p>
          {message.ok && (
            <Link
              href="/shop/basket"
              className="text-xs text-blue-600 hover:underline dark:text-blue-400"
            >
              買い物かごを見る →
            </Link>
          )}
        </div>
      )}
    </div>
  )
}

function StockStatus({ available }: { available: number }) {
  if (available <= 0) {
    return <p className="text-sm font-medium text-red-600 dark:text-red-400">在庫切れ</p>
  }
  if (available <= LOW_STOCK_THRESHOLD) {
    return (
      <p className="text-sm font-medium text-orange-600 dark:text-orange-400">
        残り{available.toLocaleString("ja-JP")}点 ご注文はお早めに
      </p>
    )
  }
  return <p className="text-sm font-medium text-green-600 dark:text-green-400">在庫あり</p>
}
