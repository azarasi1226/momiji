"use client"

import { type ChangeEvent } from "react"
import { usePathname, useRouter, useSearchParams } from "next/navigation"

/**
 * URL クエリ駆動の汎用セレクト。 変更した瞬間に該当クエリ（[param]）だけ差し替えて再取得する。
 *
 * - 他のクエリ（検索語など）は維持し、 変更時は `page` を消して 1 ページ目に戻す。
 * - 値が空文字の選択肢はそのクエリを**外す**（例: 状態フィルタの「すべて」）。
 * - `usePathname` を使うのでどの一覧ページでも使い回せる。
 */
export function QueryParamSelect({
  param,
  value,
  label,
  options,
}: {
  param: string
  value: string
  label: string
  options: { value: string; label: string }[]
}) {
  const router = useRouter()
  const pathname = usePathname()
  const searchParams = useSearchParams()

  function handleChange(e: ChangeEvent<HTMLSelectElement>) {
    const params = new URLSearchParams(searchParams.toString())
    if (e.target.value) {
      params.set(param, e.target.value)
    } else {
      params.delete(param)
    }
    params.delete("page")
    router.push(`${pathname}?${params.toString()}`)
  }

  return (
    <div className="flex flex-col gap-1">
      <label htmlFor={param} className="text-xs text-zinc-500 dark:text-zinc-400">
        {label}
      </label>
      <select
        id={param}
        value={value}
        onChange={handleChange}
        className="h-10 rounded-lg border border-zinc-200 px-3 text-sm dark:border-zinc-700 dark:bg-zinc-900 dark:text-zinc-50"
      >
        {options.map((opt) => (
          <option key={opt.value} value={opt.value}>
            {opt.label}
          </option>
        ))}
      </select>
    </div>
  )
}
