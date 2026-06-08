"use client"

import { useState } from "react"
import { Button } from "@/components/ui/button"
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
        <Button
          type="button"
          variant="destructive"
          className="mt-2 w-fit"
          onClick={() => setConfirming(true)}
        >
          アカウントを削除する
        </Button>
      </div>
    )
  }

  return (
    <div className="flex w-full flex-col gap-4">
      <p className="text-sm text-destructive">
        本当にアカウントを削除しますか？この操作は取り消せません。
      </p>
      {error && <p className="text-sm text-destructive">{error}</p>}
      <div className="flex gap-4">
        <Button
          type="button"
          variant="destructive"
          className="flex-1"
          onClick={handleDelete}
          disabled={isPending}
        >
          {isPending ? "削除中..." : "削除する"}
        </Button>
        <Button
          type="button"
          variant="outline"
          className="flex-1"
          onClick={() => {
            setConfirming(false)
            setError(null)
          }}
          disabled={isPending}
        >
          キャンセル
        </Button>
      </div>
    </div>
  )
}
