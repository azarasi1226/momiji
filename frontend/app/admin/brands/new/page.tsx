import Link from "next/link"
import { redirect } from "next/navigation"
import { auth } from "@/auth"
import { BrandCreateForm } from "./brand-create-form"

export default async function NewBrandPage() {
  const session = await auth()
  if (!session || session.error === "RefreshTokenError") {
    redirect("/")
  }

  return (
    <div className="flex min-h-screen justify-center bg-zinc-50 font-sans dark:bg-black">
      <main className="flex w-full max-w-2xl flex-col gap-8 px-8 py-16">
        <div className="flex items-center justify-between">
          <h1 className="text-2xl font-semibold text-black dark:text-zinc-50">
            ブランド新規作成
          </h1>
          <Link
            href="/admin/brands"
            className="text-sm text-zinc-500 hover:text-zinc-800 dark:text-zinc-400 dark:hover:text-zinc-100"
          >
            戻る
          </Link>
        </div>

        <BrandCreateForm />
      </main>
    </div>
  )
}
