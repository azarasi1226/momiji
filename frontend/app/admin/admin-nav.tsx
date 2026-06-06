"use client"

import Link from "next/link"
import { usePathname } from "next/navigation"

const NAV_ITEMS = [
  { href: "/admin/brands", label: "ブランド管理" },
  { href: "/admin/products", label: "商品管理" },
]

/**
 * admin 左サイドバーのナビ。 現在地を `usePathname` でハイライトする（client component が必要な理由）。
 * 配下ページ（例 /admin/brands/123）でも親セクションを active 扱いにする。
 */
export function AdminNav() {
  const pathname = usePathname()

  return (
    <nav className="flex flex-col gap-1">
      {NAV_ITEMS.map((item) => {
        const active =
          pathname === item.href || pathname.startsWith(`${item.href}/`)
        return (
          <Link
            key={item.href}
            href={item.href}
            className={
              active
                ? "rounded-lg bg-zinc-200 px-3 py-2 text-sm font-medium text-black dark:bg-zinc-800 dark:text-zinc-50"
                : "rounded-lg px-3 py-2 text-sm text-zinc-600 transition-colors hover:bg-zinc-100 dark:text-zinc-400 dark:hover:bg-zinc-900"
            }
          >
            {item.label}
          </Link>
        )
      })}
    </nav>
  )
}
