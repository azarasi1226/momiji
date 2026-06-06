import Link from "next/link"
import { brandNameMap, listProducts } from "./actions"

export default async function ProductListPage() {
  const [products, brandNames] = await Promise.all([
    listProducts(),
    brandNameMap(),
  ])

  return (
    <main className="flex w-full max-w-5xl flex-col gap-8 px-8 py-16">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-semibold text-black dark:text-zinc-50">
          商品管理
        </h1>
        <Link
          href="/admin/products/new"
          className="flex h-10 items-center justify-center rounded-full bg-foreground px-6 text-sm text-background transition-colors hover:bg-[#383838] dark:hover:bg-[#ccc]"
        >
          新規作成
        </Link>
      </div>

      {products.length === 0 ? (
        <p className="text-sm text-zinc-500 dark:text-zinc-400">
          商品がまだ登録されていません。
        </p>
      ) : (
        <table className="w-full border-collapse text-left text-sm">
          <thead>
            <tr className="border-b border-zinc-200 text-zinc-500 dark:border-zinc-700 dark:text-zinc-400">
              <th className="py-2 pr-4 font-medium">商品名</th>
              <th className="py-2 pr-4 font-medium">ブランド</th>
              <th className="py-2 pr-4 font-medium">価格</th>
              <th className="py-2 pr-4 font-medium">状態</th>
              <th className="py-2 pr-4 font-medium">更新日時</th>
              <th className="py-2 font-medium" />
            </tr>
          </thead>
          <tbody>
            {products.map((product) => (
              <tr
                key={product.id}
                className="border-b border-zinc-100 text-black dark:border-zinc-800 dark:text-zinc-50"
              >
                <td className="py-3 pr-4">{product.name}</td>
                <td className="py-3 pr-4 text-zinc-500 dark:text-zinc-400">
                  {brandNames[product.brandId] ?? product.brandId}
                </td>
                <td className="py-3 pr-4">
                  ¥{product.price.toLocaleString("ja-JP")}
                </td>
                <td className="py-3 pr-4">
                  <span
                    className={
                      product.status === "DISCONTINUED"
                        ? "rounded-full bg-zinc-200 px-2 py-0.5 text-xs text-zinc-600 dark:bg-zinc-700 dark:text-zinc-300"
                        : "rounded-full bg-green-100 px-2 py-0.5 text-xs text-green-700 dark:bg-green-950 dark:text-green-400"
                    }
                  >
                    {product.status}
                  </span>
                </td>
                <td className="py-3 pr-4 text-zinc-500 dark:text-zinc-400">
                  {product.updatedAt
                    ? new Date(product.updatedAt).toLocaleString("ja-JP")
                    : "—"}
                </td>
                <td className="py-3 text-right">
                  <Link
                    href={`/admin/products/${product.id}`}
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
  )
}
