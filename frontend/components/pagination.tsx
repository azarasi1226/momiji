"use client"

import Link from "next/link"
import { usePathname, useSearchParams } from "next/navigation"

/**
 * 汎用ページングコンポーネント。 `currentPage` / `totalPage` を渡すだけで使える drop-in。
 *
 * - 現在の URL クエリ（q / sort 等）を保ったまま `page` だけ差し替えるので、 検索・ソート状態を維持する。
 * - ページ数が多いときは省略記号(…)で畳む（先頭・末尾・現在周辺だけ表示）。
 * - `<Link>` ベースなのでプリフェッチ可・URL 駆動（ブックマーク可）。
 */
export function Pagination({
  currentPage,
  totalPage,
  pageParam = "page",
}: {
  currentPage: number
  totalPage: number
  pageParam?: string
}) {
  const pathname = usePathname()
  const searchParams = useSearchParams()

  if (totalPage <= 1) return null

  const hrefFor = (page: number) => {
    const params = new URLSearchParams(searchParams.toString())
    params.set(pageParam, String(page))
    return `${pathname}?${params.toString()}`
  }

  const items = pageItems(currentPage, totalPage)

  return (
    <nav className="flex items-center justify-center gap-1" aria-label="ページネーション">
      <ArrowLink href={hrefFor(currentPage - 1)} disabled={currentPage <= 1} label="前のページ">
        ‹
      </ArrowLink>

      {items.map((item, i) =>
        item === ELLIPSIS ? (
          <span
            key={`ellipsis-${i}`}
            className="flex h-9 w-9 items-center justify-center text-sm text-zinc-400 dark:text-zinc-600"
          >
            …
          </span>
        ) : item === currentPage ? (
          <span
            key={item}
            aria-current="page"
            className="flex h-9 w-9 items-center justify-center rounded-lg bg-foreground text-sm font-medium text-background"
          >
            {item}
          </span>
        ) : (
          <Link
            key={item}
            href={hrefFor(item)}
            className="flex h-9 w-9 items-center justify-center rounded-lg text-sm text-zinc-700 transition-colors hover:bg-zinc-100 dark:text-zinc-200 dark:hover:bg-zinc-800"
          >
            {item}
          </Link>
        ),
      )}

      <ArrowLink
        href={hrefFor(currentPage + 1)}
        disabled={currentPage >= totalPage}
        label="次のページ"
      >
        ›
      </ArrowLink>
    </nav>
  )
}

function ArrowLink({
  href,
  disabled,
  label,
  children,
}: {
  href: string
  disabled: boolean
  label: string
  children: React.ReactNode
}) {
  if (disabled) {
    return (
      <span className="flex h-9 w-9 items-center justify-center rounded-lg text-sm text-zinc-300 dark:text-zinc-700">
        {children}
      </span>
    )
  }
  return (
    <Link
      href={href}
      aria-label={label}
      className="flex h-9 w-9 items-center justify-center rounded-lg text-sm text-zinc-700 transition-colors hover:bg-zinc-100 dark:text-zinc-200 dark:hover:bg-zinc-800"
    >
      {children}
    </Link>
  )
}

const ELLIPSIS = "…" as const

/**
 * 表示するページ項目を決める。 総ページが少なければ全部、 多ければ先頭・末尾・現在周辺 + 省略記号。
 * 例: current=7, total=20 → [1, …, 6, 7, 8, …, 20]
 */
function pageItems(current: number, total: number): (number | typeof ELLIPSIS)[] {
  const MAX_FULL = 7
  if (total <= MAX_FULL) {
    return Array.from({ length: total }, (_, i) => i + 1)
  }

  const delta = 1
  const start = Math.max(2, current - delta)
  const end = Math.min(total - 1, current + delta)

  const items: (number | typeof ELLIPSIS)[] = [1]
  if (start > 2) items.push(ELLIPSIS)
  for (let p = start; p <= end; p++) items.push(p)
  if (end < total - 1) items.push(ELLIPSIS)
  items.push(total)
  return items
}
