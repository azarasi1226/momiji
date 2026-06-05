import Link from "next/link"
import { redirect } from "next/navigation"
import { auth } from "@/auth"
import { listBrands } from "./actions"

export default async function BrandListPage() {
  const session = await auth()
  if (!session || session.error === "RefreshTokenError") {
    redirect("/")
  }

  const brands = await listBrands()

  return (
    <div className="flex min-h-screen justify-center bg-zinc-50 font-sans dark:bg-black">
      <main className="flex w-full max-w-4xl flex-col gap-8 px-8 py-16">
        <div className="flex items-center justify-between">
          <h1 className="text-2xl font-semibold text-black dark:text-zinc-50">
            ブランド管理
          </h1>
          <Link
            href="/admin/brands/new"
            className="flex h-10 items-center justify-center rounded-full bg-foreground px-6 text-sm text-background transition-colors hover:bg-[#383838] dark:hover:bg-[#ccc]"
          >
            新規作成
          </Link>
        </div>

        {brands.length === 0 ? (
          <p className="text-sm text-zinc-500 dark:text-zinc-400">
            ブランドがまだ登録されていません。
          </p>
        ) : (
          <table className="w-full border-collapse text-left text-sm">
            <thead>
              <tr className="border-b border-zinc-200 text-zinc-500 dark:border-zinc-700 dark:text-zinc-400">
                <th className="py-2 pr-4 font-medium">ブランド名</th>
                <th className="py-2 pr-4 font-medium">説明</th>
                <th className="py-2 pr-4 font-medium">更新日時</th>
                <th className="py-2 font-medium" />
              </tr>
            </thead>
            <tbody>
              {brands.map((brand) => (
                <tr
                  key={brand.id}
                  className="border-b border-zinc-100 text-black dark:border-zinc-800 dark:text-zinc-50"
                >
                  <td className="py-3 pr-4">{brand.name}</td>
                  <td className="max-w-xs truncate py-3 pr-4 text-zinc-500 dark:text-zinc-400">
                    {brand.description || "—"}
                  </td>
                  <td className="py-3 pr-4 text-zinc-500 dark:text-zinc-400">
                    {brand.updatedAt
                      ? new Date(brand.updatedAt).toLocaleString("ja-JP")
                      : "—"}
                  </td>
                  <td className="py-3 text-right">
                    <Link
                      href={`/admin/brands/${brand.id}`}
                      className="text-sm text-blue-600 hover:underline dark:text-blue-400"
                    >
                      編集
                    </Link>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </main>
    </div>
  )
}
