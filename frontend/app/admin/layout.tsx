import Link from "next/link"
import { redirect } from "next/navigation"
import { auth } from "@/auth"
import { AdminNav } from "./admin-nav"

/**
 * /admin/* 共通シェル。 左サイドバー（常設ナビ）+ 右にページ内容。
 *
 * 認証ゲートをここに集約する: layout は配下の全 /admin ページの親として先に実行され、
 * redirect すればページはレンダリングされない。 これにより各ページの auth チェック重複を無くす。
 * サイドバーは layout なのでページ遷移しても再描画されず保持される。
 */
export default async function AdminLayout({
  children,
}: {
  children: React.ReactNode
}) {
  const session = await auth()
  if (!session || session.error === "RefreshTokenError") {
    redirect("/")
  }

  return (
    <div className="flex min-h-screen bg-muted/30 font-sans">
      <aside className="flex w-60 shrink-0 flex-col gap-6 border-r bg-background px-4 py-8">
        <Link
          href="/"
          className="px-3 text-sm text-muted-foreground transition-colors hover:text-foreground"
        >
          ← ホーム
        </Link>
        <div className="flex flex-col gap-2">
          <p className="px-3 text-xs font-semibold tracking-wide text-muted-foreground">
            マスタ管理
          </p>
          <AdminNav />
        </div>
      </aside>
      <div className="flex-1">{children}</div>
    </div>
  )
}
