"use client"

import { useState, useTransition } from "react"
import { setBasketItem } from "../actions"

/**
 * 一覧カード用の「カートに入れる」（数量 1 固定）。
 * 数量指定は商品詳細ページで行う方針なので、 ここはワンタップ追加だけ。
 */
export function QuickAddButton({ productId }: { productId: string }) {
  const [isPending, startTransition] = useTransition()
  const [message, setMessage] = useState<{ ok: boolean; text: string } | null>(null)

  function handleAdd() {
    setMessage(null)
    startTransition(async () => {
      const result = await setBasketItem(productId, 1)
      setMessage(
        result?.error
          ? { ok: false, text: result.error }
          : { ok: true, text: "カートに入れました" },
      )
    })
  }

  return (
    <div className="flex flex-col gap-1">
      <button
        type="button"
        onClick={handleAdd}
        disabled={isPending}
        className="flex h-10 items-center justify-center rounded-full bg-foreground px-4 text-sm text-background transition-colors hover:bg-[#383838] disabled:opacity-50 dark:hover:bg-[#ccc]"
      >
        {isPending ? "追加中..." : "カートに入れる"}
      </button>
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
