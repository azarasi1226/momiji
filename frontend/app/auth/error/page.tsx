import Link from "next/link"

const messages: Record<string, string> = {
  no_token: "認証トークンが取得できませんでした。",
  backend: "サーバーとの通信に失敗しました。バックエンドが起動しているか確認してください。",
}

export default async function AuthErrorPage({
  searchParams,
}: {
  searchParams: Promise<{
    reason?: string
    code?: string
    message?: string
    correlationId?: string
  }>
}) {
  const { reason, code, message: detailMessage, correlationId } = await searchParams
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

        {(detailMessage || code || correlationId) && (
          <div className="w-full rounded-lg border border-zinc-200 bg-zinc-50 p-4 text-sm dark:border-zinc-800 dark:bg-zinc-900">
            <p className="mb-2 font-semibold text-zinc-700 dark:text-zinc-300">エラー詳細</p>
            <dl className="grid grid-cols-[auto_1fr] gap-x-3 gap-y-1 text-zinc-600 dark:text-zinc-400">
              {code && (
                <>
                  <dt className="font-mono text-zinc-500">code</dt>
                  <dd className="font-mono break-all">{code}</dd>
                </>
              )}
              {detailMessage && (
                <>
                  <dt className="font-mono text-zinc-500">message</dt>
                  <dd className="break-all">{detailMessage}</dd>
                </>
              )}
              {correlationId && (
                <>
                  <dt className="font-mono text-zinc-500">correlationId</dt>
                  <dd className="font-mono break-all">{correlationId}</dd>
                </>
              )}
            </dl>
            {correlationId && (
              <p className="mt-3 text-xs text-zinc-500">
                サポートに問い合わせる際はこの correlationId をお伝えください。
              </p>
            )}
          </div>
        )}

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
