import Link from "next/link"
import { BrandCreateForm } from "./brand-create-form"

export default function NewBrandPage() {
  return (
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
  )
}
