"use client"

import { useState, useTransition } from "react"
import { archiveBrand } from "../actions"

export function ArchiveBrandButton({ id }: { id: string }) {
  const [confirming, setConfirming] = useState(false)
  const [isPending, startTransition] = useTransition()

  if (!confirming) {
    return (
      <button
        type="button"
        onClick={() => setConfirming(true)}
        className="flex h-12 w-fit items-center justify-center rounded-full border border-red-500 px-8 text-sm text-red-500 transition-colors hover:bg-red-50 dark:hover:bg-red-950"
      >
        このブランドをアーカイブ
      </button>
    )
  }

  return (
    <div className="flex flex-col gap-3">
      <p className="text-sm text-red-500 dark:text-red-400">
        アーカイブすると新規商品を紐づけられなくなります。 紐づく商品は残ります。
      </p>
      <div className="flex gap-3">
        <button
          type="button"
          disabled={isPending}
          onClick={() => startTransition(() => archiveBrand(id))}
          className="flex h-12 items-center justify-center rounded-full bg-red-500 px-8 text-sm text-white transition-colors hover:bg-red-600 disabled:opacity-50"
        >
          {isPending ? "アーカイブ中..." : "アーカイブする"}
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
