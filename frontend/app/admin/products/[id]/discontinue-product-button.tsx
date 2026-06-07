"use client"

import { useState, useTransition } from "react"
import { discontinueProduct } from "../actions"

export function DiscontinueProductButton({ id }: { id: string }) {
  const [confirming, setConfirming] = useState(false)
  const [isPending, startTransition] = useTransition()

  if (!confirming) {
    return (
      <button
        type="button"
        onClick={() => setConfirming(true)}
        className="flex h-12 w-fit items-center justify-center rounded-full border border-red-500 px-8 text-sm text-red-500 transition-colors hover:bg-red-50 dark:hover:bg-red-950"
      >
        この商品を生産終了にする
      </button>
    )
  }

  return (
    <div className="flex flex-col gap-3">
      <p className="text-sm text-red-500 dark:text-red-400">
        生産終了にすると販売・編集ができなくなります。 記録は残ります。
      </p>
      <div className="flex gap-3">
        <button
          type="button"
          disabled={isPending}
          onClick={() => startTransition(() => discontinueProduct(id))}
          className="flex h-12 items-center justify-center rounded-full bg-red-500 px-8 text-sm text-white transition-colors hover:bg-red-600 disabled:opacity-50"
        >
          {isPending ? "処理中..." : "生産終了にする"}
        </button>
        <button
          type="button"
          disabled={isPending}
          onClick={() => setConfirming(false)}
          className="flex h-12 items-center justify-center rounded-full border border-zinc-200 px-8 text-sm text-zinc-500 transition-colors hover:bg-zinc-100 disabled:opacity-50 dark:border-zinc-700 dark:hover:bg-zinc-900"
        >
          キャンセル
        </button>
      </div>
    </div>
  )
}
