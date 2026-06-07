import Link from "next/link"
import { redirect } from "next/navigation"
import { auth } from "@/auth"

/**
 * /shop/* 共通シェル（購入者向け）。 上部ヘッダー（商品一覧 / カゴ）+ ページ内容。
 *
 * 認証ゲートをここに集約する: カゴは JWT から持ち主を解決するためログイン必須。
 * layout は配下ページの親として先に実行され、redirect すればページは描画されない。
 */
export default async function ShopLayout({
  children,
}: {
  children: React.ReactNode
}) {
  const session = await auth()
  if (!session || session.error === "RefreshTokenError") {
    redirect("/")
  }

  return (
    <div className="flex min-h-screen flex-col bg-zinc-50 font-sans dark:bg-black">
      <header className="sticky top-0 z-10 flex items-center justify-between border-b border-zinc-200 bg-white/80 px-8 py-4 backdrop-blur dark:border-zinc-800 dark:bg-black/80">
        <div className="flex items-center gap-6">
          <Link
            href="/shop/products"
            className="text-lg font-semibold text-black dark:text-zinc-50"
          >
            momiji shop
          </Link>
          <Link
            href="/shop/products"
            className="text-sm text-zinc-600 transition-colors hover:text-black dark:text-zinc-400 dark:hover:text-zinc-50"
          >
            商品一覧
          </Link>
        </div>
        <div className="flex items-center gap-3">
          <Link
            href="/shop/basket"
            className="flex h-10 items-center justify-center gap-2 rounded-full border border-zinc-200 px-5 text-sm text-zinc-700 transition-colors hover:bg-zinc-100 dark:border-zinc-700 dark:text-zinc-200 dark:hover:bg-zinc-900"
          >
            🛒 買い物かご
          </Link>
          <ProfileAvatar
            name={session.user?.name}
            email={session.user?.email}
            image={session.user?.image}
          />
        </div>
      </header>
      <div className="flex flex-1 justify-center">{children}</div>
    </div>
  )
}

/**
 * 右上のプロフィールアイコン。 IdP のアバター画像があれば表示し、 無ければ
 * 名前/メールの頭文字を丸アイコンで出す。 クリックで /profile へ遷移。
 */
function ProfileAvatar({
  name,
  email,
  image,
}: {
  name?: string | null
  email?: string | null
  image?: string | null
}) {
  const label = name || email || "プロフィール"
  const initial = (name || email || "?").charAt(0).toUpperCase()

  return (
    <Link
      href="/profile"
      aria-label="プロフィール"
      title={label}
      className="flex h-10 w-10 items-center justify-center overflow-hidden rounded-full border border-zinc-200 bg-zinc-100 text-sm font-medium text-zinc-700 transition-colors hover:bg-zinc-200 dark:border-zinc-700 dark:bg-zinc-800 dark:text-zinc-200 dark:hover:bg-zinc-700"
    >
      {image ? (
        // eslint-disable-next-line @next/next/no-img-element
        <img src={image} alt={label} className="h-full w-full object-cover" />
      ) : (
        initial
      )}
    </Link>
  )
}
