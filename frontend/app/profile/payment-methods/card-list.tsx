"use client"

import { useState, useTransition } from "react"
import { useRouter } from "next/navigation"
import { Button } from "@/components/ui/button"
import { changeDefaultCard, deleteCard, type Card } from "./actions"

// ブランドごとの券面デザイン。 未知ブランドは FALLBACK に落ちる。
const BRAND_STYLES: Record<string, { gradient: string; label: string }> = {
  visa: { gradient: "from-blue-800 via-blue-600 to-indigo-700", label: "VISA" },
  mastercard: { gradient: "from-zinc-900 via-red-950 to-orange-900", label: "Mastercard" },
  jcb: { gradient: "from-emerald-800 via-teal-700 to-cyan-800", label: "JCB" },
  amex: { gradient: "from-teal-700 via-cyan-700 to-sky-800", label: "American Express" },
}
const FALLBACK_STYLE = { gradient: "from-slate-700 via-slate-600 to-slate-800", label: "" }

export function CardList({ cards }: { cards: Card[] }) {
  const router = useRouter()
  const [isPending, startTransition] = useTransition()
  const [error, setError] = useState<string | null>(null)

  // 楽観的オーバーレイ。
  // server action の成功 = イベントはストアに確定済みだが、 一覧の元になる ReadModel（projection）は
  // 非同期なので、 直後の router.refresh() が「更新前」を読むことがある（read-your-writes レース）。
  // クライアントは操作結果を確定的に知っているので、 ここで即座に上書き描画し、 サーバーは eventually 追いつく。
  const [defaultOverride, setDefaultOverride] = useState<string | null>(null)
  const [deletedIds, setDeletedIds] = useState<ReadonlySet<string>>(new Set())

  const displayCards = cards
    .filter((card) => !deletedIds.has(card.id))
    .map((card) => (defaultOverride ? { ...card, isDefault: card.id === defaultOverride } : card))

  function onSetDefault(id: string) {
    startTransition(async () => {
      setError(null)
      const result = await changeDefaultCard(id)
      if (result?.error) {
        setError(result.error)
        return
      }
      setDefaultOverride(id)
      router.refresh()
    })
  }

  function onDelete(id: string) {
    startTransition(async () => {
      setError(null)
      const result = await deleteCard(id)
      if (result?.error) {
        setError(result.error)
        return
      }
      const deleted = displayCards.find((card) => card.id === id)
      const survivors = displayCards.filter((card) => card.id !== id)
      // backend は「default を消したら最古の残カードを昇格」する（一覧は登録順なので先頭 = 最古）。
      // 同じルールをここでも適用して、 projection を待たずに昇格結果を描画する。
      if (deleted?.isDefault && survivors.length > 0) {
        setDefaultOverride(survivors[0].id)
      }
      setDeletedIds((prev) => new Set(prev).add(id))
      router.refresh()
    })
  }

  if (displayCards.length === 0) {
    return <p className="text-sm text-muted-foreground">登録済みのカードはありません。</p>
  }

  return (
    <div className="flex flex-col gap-4">
      <ul className="grid gap-6 sm:grid-cols-2">
        {displayCards.map((card) => (
          <CardItem
            key={card.id}
            card={card}
            isPending={isPending}
            onSetDefault={() => onSetDefault(card.id)}
            onDelete={() => onDelete(card.id)}
          />
        ))}
      </ul>
      {error && <p className="text-sm text-destructive">{error}</p>}
    </div>
  )
}

function CardItem({
  card,
  isPending,
  onSetDefault,
  onDelete,
}: {
  card: Card
  isPending: boolean
  onSetDefault: () => void
  onDelete: () => void
}) {
  const style = BRAND_STYLES[card.brand.toLowerCase()] ?? FALLBACK_STYLE
  const expiry = `${String(card.expMonth).padStart(2, "0")}/${String(card.expYear % 100).padStart(2, "0")}`

  return (
    <li className="flex flex-col gap-2">
      {/* 券面 */}
      <div
        className={`relative flex aspect-[1.586/1] w-full flex-col justify-between rounded-2xl bg-gradient-to-br p-5 text-white shadow-lg ${style.gradient} ${
          card.isDefault ? "ring-2 ring-primary ring-offset-2 ring-offset-background" : ""
        }`}
      >
        {/* 上段: チップ + デフォルトバッジ */}
        <div className="flex items-start justify-between">
          <div className="h-8 w-11 rounded-md border border-yellow-200/40 bg-gradient-to-br from-yellow-200 to-yellow-500/80" />
          {card.isDefault && (
            <span className="rounded-full bg-white/20 px-2.5 py-1 text-xs font-medium backdrop-blur-sm">
              デフォルト
            </span>
          )}
        </div>

        {/* 中段: カード番号（下4桁のみ実数） */}
        <p className="font-mono text-xl tracking-[0.2em] drop-shadow-sm">
          •••• •••• •••• {card.last4}
        </p>

        {/* 下段: 有効期限 + ブランド */}
        <div className="flex items-end justify-between">
          <div>
            <p className="text-[10px] uppercase tracking-wider text-white/60">有効期限</p>
            <p className="font-mono text-sm">{expiry}</p>
          </div>
          <p className="text-lg font-bold italic tracking-wide drop-shadow-sm">
            {style.label || card.brand}
          </p>
        </div>
      </div>

      {/* 操作列 */}
      <div className="flex items-center justify-end gap-2">
        {!card.isDefault && (
          <Button type="button" variant="outline" size="sm" onClick={onSetDefault} disabled={isPending}>
            デフォルトにする
          </Button>
        )}
        <Button type="button" variant="destructive" size="sm" onClick={onDelete} disabled={isPending}>
          削除
        </Button>
      </div>
    </li>
  )
}
