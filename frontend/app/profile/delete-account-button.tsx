"use client"

import { useState } from "react"
import { deleteAccount } from "./actions"

export function DeleteAccountButton() {
  const [confirming, setConfirming] = useState(false)
  const [isPending, setIsPending] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function handleDelete() {
    setIsPending(true)
    setError(null)
    const result = await deleteAccount()
    if (result?.error) {
      setError(result.error)
      setIsPending(false)
      setConfirming(false)
    }
  }

  if (!confirming) {
    return (
      <div className="flex w-full flex-col gap-2">
        <button
          type="button"
          onClick={() => setConfirming(true)}
          className="mt-2 flex h-12 items-center justify-center rounded-full border border-red-500 px-8 text-red-500 transition-colors hover:bg-red-50 dark:hover:bg-red-950"
        >
          アカウントを削除する
        </button>
      </div>
    )
  }

  return (
    <div className="flex w-full flex-col gap-4">
      <p className="text-sm text-red-500">
        本当にアカウントを削除しますか？この操作は取り消せません。
      </p>
      {error && <p className="text-sm text-red-500">{error}</p>}
      <div className="flex gap-4">
        <button
          type="button"
          onClick={handleDelete}
          disabled={isPending}
          className="flex h-12 flex-1 items-center justify-center rounded-full bg-red-500 px-8 text-white transition-colors hover:bg-red-600 disabled:opacity-50"
        >
          {isPending ? "削除中..." : "削除する"}
        </button>
        <button
          type="button"
          onClick={() => { setConfirming(false); setError(null) }}
          disabled={isPending}
          className="flex h-12 flex-1 items-center justify-center rounded-full border border-zinc-200 px-8 transition-colors hover:bg-zinc-50 dark:border-zinc-700 dark:hover:bg-zinc-800"
        >
          キャンセル
        </button>
      </div>
    </div>
  )
}
