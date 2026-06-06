import Link from "next/link"
import { redirect } from "next/navigation"
import { auth } from "@/auth"
import { fetchBrand } from "../actions"
import { BrandEditForm } from "./brand-edit-form"
import { ArchiveBrandButton } from "./archive-brand-button"

export default async function BrandDetailPage({
  params,
}: {
  params: Promise<{ id: string }>
}) {
  const session = await auth()
  if (!session || session.error === "RefreshTokenError") {
    redirect("/")
  }

  const { id } = await params
  const brand = await fetchBrand(id)

  return (
    <div className="flex min-h-screen justify-center bg-zinc-50 font-sans dark:bg-black">
      <main className="flex w-full max-w-2xl flex-col gap-8 px-8 py-16">
        <div className="flex items-center justify-between">
          <h1 className="text-2xl font-semibold text-black dark:text-zinc-50">
            ブランド編集
          </h1>
          <Link
            href="/admin/brands"
            className="text-sm text-zinc-500 hover:text-zinc-800 dark:text-zinc-400 dark:hover:text-zinc-100"
          >
            戻る
          </Link>
        </div>

        <p className="text-xs text-zinc-400 dark:text-zinc-500">
          ID: {brand.id} ／ 状態: {brand.status}
        </p>

        <BrandEditForm brand={brand} />

        <hr className="w-full border-zinc-200 dark:border-zinc-700" />

        <ArchiveBrandButton id={brand.id} />
      </main>
    </div>
  )
}
