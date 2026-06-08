"use client"

import { useState, useTransition } from "react"
import { Button } from "@/components/ui/button"
import { archiveBrand } from "../actions"

export function ArchiveBrandButton({ id }: { id: string }) {
  const [confirming, setConfirming] = useState(false)
  const [isPending, startTransition] = useTransition()

  if (!confirming) {
    return (
      <Button
        type="button"
        variant="destructive"
        className="w-fit"
        onClick={() => setConfirming(true)}
      >
        このブランドをアーカイブ
      </Button>
    )
  }

  return (
    <div className="flex flex-col gap-3">
      <p className="text-sm text-destructive">
        アーカイブすると新規商品を紐づけられなくなります。 紐づく商品は残ります。
      </p>
      <div className="flex gap-3">
        <Button
          type="button"
          variant="destructive"
          disabled={isPending}
          onClick={() => startTransition(() => archiveBrand(id))}
        >
          {isPending ? "アーカイブ中..." : "アーカイブする"}
        </Button>
        <Button
          type="button"
          variant="outline"
          disabled={isPending}
          onClick={() => setConfirming(false)}
        >
          キャンセル
        </Button>
      </div>
    </div>
  )
}
