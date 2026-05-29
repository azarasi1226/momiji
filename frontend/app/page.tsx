import Link from "next/link"
import { auth, signIn, signOut } from "@/auth"

export default async function Home() {
  const session = await auth()
  const sessionExpired = session?.error === "RefreshTokenError"
  // 期限切れ session は「ログイン中」 として扱わない。 そうしないと profile への遷移で
  // 「/profile → / リダイレクト → / でログイン中 UI 表示」 のループにハマる。
  const isLoggedIn = !!session && !sessionExpired

  return (
    <div className="flex min-h-screen items-center justify-center bg-zinc-50 font-sans dark:bg-black">
      <main className="flex min-h-screen w-full max-w-3xl flex-col items-center justify-center gap-8 py-32 px-16 bg-white dark:bg-black">
        {isLoggedIn ? (
          <>
            <h1 className="text-2xl font-semibold text-black dark:text-zinc-50">
              ログイン中
            </h1>
            <div className="text-zinc-600 dark:text-zinc-400 text-center">
              <p>{session.user?.name}</p>
              <p>{session.user?.email}</p>
            </div>
            <div className="flex gap-4">
              <Link
                href="/profile"
                className="flex h-12 items-center justify-center rounded-full bg-foreground px-8 text-background transition-colors hover:bg-[#383838] dark:hover:bg-[#ccc]"
              >
                プロフィール
              </Link>
              <form
                action={async () => {
                  "use server"
                  await signOut()
                }}
              >
                <button
                  type="submit"
                  className="flex h-12 items-center justify-center rounded-full border border-solid border-black/[.08] px-8 transition-colors hover:border-transparent hover:bg-black/[.04] dark:border-white/[.145] dark:hover:bg-[#1a1a1a]"
                >
                  ログアウト
                </button>
              </form>
            </div>
          </>
        ) : (
          <>
            <h1 className="text-2xl font-semibold text-black dark:text-zinc-50">
              momiji
            </h1>
            {sessionExpired && (
              <p className="text-sm text-red-600 dark:text-red-400">
                セッションの有効期限が切れました。 再度ログインしてください。
              </p>
            )}
            <form
              action={async () => {
                "use server"
                // 期限切れ session には古い token が残っているので、 先にクリアしてから signIn する。
                // そうしないと NextAuth が古い jwt を引き続き使おうとして再ログインが効かないことがある。
                if (sessionExpired) {
                  await signOut({ redirect: false })
                }
                await signIn("keycloak")
              }}
            >
              <button
                type="submit"
                className="flex h-12 items-center justify-center gap-2 rounded-full bg-foreground px-8 text-background transition-colors hover:bg-[#383838] dark:hover:bg-[#ccc]"
              >
                ログイン
              </button>
            </form>
          </>
        )}
      </main>
    </div>
  )
}
