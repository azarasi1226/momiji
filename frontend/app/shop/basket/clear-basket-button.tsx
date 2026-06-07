"use client"

import { useState, useTransition } from "react"
import { clearBasket } from "../actions"

/** カゴを空にする。 誤操作防止に確認を挟む。 */
export function ClearBasketButton() {
  const [confirming, setConfirming] = useState(false)
  const [isPending, startTransition] = useTransition()
  const [error, setError] = useState<string | null>(null)

  function handleClear() {
    setError(null)
    startTransition(async () => {
      const result = await clearBasket()
      if (result?.error) {
        setError(result.error)
        setConfirming(false)
      }
    })
  }

  if (!confirming) {
    return (
      <button
        type="button"
        onClick={() => setConfirming(true)}
        className="text-sm text-red-600 transition-colors hover:underline dark:text-red-400"
      >
        カゴを空にする
      </button>
    )
  }

  return (
    <div className="flex items-center gap-3">
      {error && <span className="text-xs text-red-600 dark:text-red-400">{error}</span>}
      <span className="text-sm text-zinc-600 dark:text-zinc-400">本当に空にしますか？</span>
      <button
        type="button"
        onClick={handleClear}
        disabled={isPending}
        className="h-9 rounded-full bg-red-500 px-4 text-xs text-white transition-colors hover:bg-red-600 disabled:opacity-50"
      >
        {isPending ? "処理中..." : "空にする"}
      </button>
      <button
        type="button"
        onClick={() => setConfirming(false)}
        disabled={isPending}
        className="h-9 rounded-full border border-zinc-200 px-4 text-xs text-zinc-700 transition-colors hover:bg-zinc-100 dark:border-zinc-700 dark:text-zinc-200 dark:hover:bg-zinc-900"
      >
        キャンセル
      </button>
    </div>
  )
}
