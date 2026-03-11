import Link from "next/link"

const messages: Record<string, string> = {
  no_token: "認証トークンが取得できませんでした。",
  backend: "サーバーとの通信に失敗しました。バックエンドが起動しているか確認してください。",
}

export default async function AuthErrorPage({
  searchParams,
}: {
  searchParams: Promise<{ reason?: string }>
}) {
  const { reason } = await searchParams
  const message = messages[reason ?? ""] ?? "ログイン中にエラーが発生しました。"

  return (
    <div className="flex min-h-screen items-center justify-center bg-zinc-50 font-sans dark:bg-black">
      <main className="flex w-full max-w-3xl flex-col items-center gap-8 py-32 px-16 bg-white dark:bg-black">
        <h1 className="text-2xl font-semibold text-black dark:text-zinc-50">
          ログインに失敗しました
        </h1>
        <p className="text-zinc-600 dark:text-zinc-400 text-center">
          {message}
        </p>
        <Link
          href="/"
          className="flex h-12 items-center justify-center rounded-full bg-foreground px-8 text-background transition-colors hover:bg-[#383838] dark:hover:bg-[#ccc]"
        >
          トップに戻る
        </Link>
      </main>
    </div>
  )
}
