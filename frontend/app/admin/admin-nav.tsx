"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { cn } from "@/lib/utils";

const NAV_ITEMS = [
  { href: "/admin/brands", label: "ブランド管理" },
  { href: "/admin/products", label: "商品管理" },
  { href: "/admin/orders", label: "発送管理" },
];

/**
 * admin 左サイドバーのナビ。 現在地を `usePathname` でハイライトする（client component が必要な理由）。
 * 配下ページ（例 /admin/brands/123）でも親セクションを active 扱いにする。
 */
export function AdminNav() {
  const pathname = usePathname();

  return (
    <nav className="flex flex-col gap-1">
      {NAV_ITEMS.map((item) => {
        const active =
          pathname === item.href || pathname.startsWith(`${item.href}/`);
        return (
          <Link
            key={item.href}
            href={item.href}
            className={cn(
              "rounded-lg px-3 py-2 text-sm transition-colors",
              active
                ? "bg-muted font-medium text-foreground"
                : "text-muted-foreground hover:bg-muted/60",
            )}
          >
            {item.label}
          </Link>
        );
      })}
    </nav>
  );
}
