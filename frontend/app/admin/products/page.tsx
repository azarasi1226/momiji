import Link from "next/link"
import { Pagination } from "@/components/pagination"
import { QueryParamSelect } from "@/components/query-param-select"
import { formatDateTime } from "@/lib/format"
import { productStatusLabel } from "@/lib/status-labels"
import { listAllBrands, listProducts } from "./actions"

const PAGE_SIZE = 20

const SORT_OPTIONS = [
  { value: "name_asc", label: "名前 昇順" },
  { value: "name_desc", label: "名前 降順" },
  { value: "price_asc", label: "価格 安い順" },
  { value: "price_desc", label: "価格 高い順" },
  { value: "created_desc", label: "新しい順" },
  { value: "created_asc", label: "古い順" },
]

const STATUS_OPTIONS = [
  { value: "", label: "すべて" },
  { value: "ACTIVE", label: "販売中" },
  { value: "DISCONTINUED", label: "生産終了" },
]

export default async function ProductListPage({
  searchParams,
}: {
  searchParams: Promise<{
    q?: string
    status?: string
    brand?: string
    sort?: string
    page?: string
  }>
}) {
  const sp = await searchParams
  const likeName = sp.q ?? ""
  const status = sp.status ?? ""
  const brandId = sp.brand ?? ""
  const sort = sp.sort ?? "name_asc"
  const pageNumber = Math.max(1, Number(sp.page ?? "1") || 1)

  const [page, brands] = await Promise.all([
    listProducts({ likeName, status, brandId, sort, pageSize: PAGE_SIZE, pageNumber }),
    listAllBrands(),
  ])

  const brandNames = Object.fromEntries(brands.map((b) => [b.id, b.name]))
  const brandOptions = [
    { value: "", label: "すべて" },
    ...brands.map((b) => ({ value: b.id, label: b.name })),
  ]

  return (
    <main className="flex w-full max-w-5xl flex-col gap-6 px-8 py-16">
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

      {/* 左: 絞り込み（商品名・状態）を検索ボタンで適用。 右: 並び順（変更で即適用）。 */}
      <div className="flex flex-wrap items-end justify-between gap-3">
        <form method="get" className="flex flex-wrap items-end gap-3">
          <div className="flex flex-col gap-1">
            <label
              htmlFor="q"
              className="text-xs text-zinc-500 dark:text-zinc-400"
            >
              商品名で検索
            </label>
            <input
              id="q"
              name="q"
              type="text"
              defaultValue={likeName}
              placeholder="部分一致"
              className="h-10 rounded-lg border border-zinc-200 px-3 text-sm dark:border-zinc-700 dark:bg-zinc-900 dark:text-zinc-50"
            />
          </div>
          <div className="flex flex-col gap-1">
            <label
              htmlFor="status"
              className="text-xs text-zinc-500 dark:text-zinc-400"
            >
              状態
            </label>
            <select
              id="status"
              name="status"
              defaultValue={status}
              className="h-10 rounded-lg border border-zinc-200 px-3 text-sm dark:border-zinc-700 dark:bg-zinc-900 dark:text-zinc-50"
            >
              {STATUS_OPTIONS.map((opt) => (
                <option key={opt.value} value={opt.value}>
                  {opt.label}
                </option>
              ))}
            </select>
          </div>
          <div className="flex flex-col gap-1">
            <label
              htmlFor="brand"
              className="text-xs text-zinc-500 dark:text-zinc-400"
            >
              ブランド
            </label>
            <select
              id="brand"
              name="brand"
              defaultValue={brandId}
              className="h-10 rounded-lg border border-zinc-200 px-3 text-sm dark:border-zinc-700 dark:bg-zinc-900 dark:text-zinc-50"
            >
              {brandOptions.map((opt) => (
                <option key={opt.value} value={opt.value}>
                  {opt.label}
                </option>
              ))}
            </select>
          </div>
          {/* 検索時に現在の並び順を維持する */}
          <input type="hidden" name="sort" value={sort} />
          <button
            type="submit"
            className="h-10 rounded-full border border-zinc-200 px-6 text-sm text-zinc-700 transition-colors hover:bg-zinc-100 dark:border-zinc-700 dark:text-zinc-200 dark:hover:bg-zinc-900"
          >
            検索
          </button>
        </form>

        <QueryParamSelect
          param="sort"
          value={sort}
          label="並び順"
          options={SORT_OPTIONS}
        />
      </div>

      {page.products.length === 0 ? (
        <p className="text-sm text-zinc-500 dark:text-zinc-400">
          条件に一致する商品がありません。
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
            {page.products.map((product) => (
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
                    {productStatusLabel(product.status)}
                  </span>
                </td>
                <td className="py-3 pr-4 text-zinc-500 dark:text-zinc-400">
                  {formatDateTime(product.updatedAt)}
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

      {/* ページング（中央の番号付き）+ 件数 */}
      <div className="flex flex-col items-center gap-3">
        <Pagination currentPage={page.pageNumber} totalPage={page.totalPage} />
        <p className="text-xs text-zinc-500 dark:text-zinc-400">
          全 {page.totalCount.toLocaleString("ja-JP")} 件
        </p>
      </div>
    </main>
  )
}
