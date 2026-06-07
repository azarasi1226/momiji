"use client"

import { usePathname, useRouter, useSearchParams } from "next/navigation"
import { Label } from "@/components/ui/label"
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"

/**
 * URL クエリ駆動の汎用セレクト。 変更した瞬間に該当クエリ（[param]）だけ差し替えて再取得する。
 *
 * - 他のクエリ（検索語など）は維持し、 変更時は `page` を消して 1 ページ目に戻す。
 * - 値が空文字の選択肢はそのクエリを**外す**（例: 状態フィルタの「すべて」）。空値は Select の内部表現に使えないので "__all__" を噛ませる。
 * - `usePathname` を使うのでどの一覧ページでも使い回せる。
 */
const EMPTY_VALUE = "__all__"

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

  function handleChange(next: string) {
    const params = new URLSearchParams(searchParams.toString())
    if (next && next !== EMPTY_VALUE) {
      params.set(param, next)
    } else {
      params.delete(param)
    }
    params.delete("page")
    router.push(`${pathname}?${params.toString()}`)
  }

  return (
    <div className="flex flex-col gap-1">
      <Label htmlFor={param} className="text-xs text-muted-foreground">
        {label}
      </Label>
      <Select value={value || EMPTY_VALUE} onValueChange={handleChange}>
        <SelectTrigger id={param} size="sm" className="w-40">
          <SelectValue />
        </SelectTrigger>
        <SelectContent>
          {options.map((opt) => (
            <SelectItem key={opt.value} value={opt.value || EMPTY_VALUE}>
              {opt.label}
            </SelectItem>
          ))}
        </SelectContent>
      </Select>
    </div>
  )
}
