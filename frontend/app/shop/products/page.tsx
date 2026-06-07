/* eslint-disable @next/next/no-img-element */
import Link from "next/link"
import { Pagination } from "@/components/pagination"
import { QueryParamSelect } from "@/components/query-param-select"
import { listShopProducts } from "../actions"
import { QuickAddButton } from "./quick-add-button"

const PAGE_SIZE = 20

const SORT_OPTIONS = [
  { value: "name_asc", label: "名前 昇順" },
  { value: "name_desc", label: "名前 降順" },
  { value: "price_asc", label: "価格 安い順" },
  { value: "price_desc", label: "価格 高い順" },
  { value: "created_desc", label: "新しい順" },
  { value: "created_asc", label: "古い順" },
]

export default async function ShopProductListPage({
  searchParams,
}: {
  searchParams: Promise<{ q?: string; sort?: string; instock?: string; page?: string }>
}) {
  const sp = await searchParams
  const likeName = sp.q ?? ""
  const sort = sp.sort ?? "name_asc"
  const inStockOnly = sp.instock === "1"
  const pageNumber = Math.max(1, Number(sp.page ?? "1") || 1)

  const page = await listShopProducts({
    likeName,
    sort,
    inStockOnly,
    pageSize: PAGE_SIZE,
    pageNumber,
  })

  return (
    <main className="flex w-full max-w-5xl flex-col gap-6 px-8 py-12">
      <h1 className="text-2xl font-semibold text-black dark:text-zinc-50">商品一覧</h1>

      {/* 左: 商品名検索（検索ボタンで適用）。 右: 並び順（変更で即適用）。 */}
      <div className="flex flex-wrap items-end justify-between gap-3">
        <form method="get" className="flex flex-wrap items-end gap-3">
          <div className="flex flex-col gap-1">
            <label htmlFor="q" className="text-xs text-zinc-500 dark:text-zinc-400">
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
          <input type="hidden" name="sort" value={sort} />
          {/* 在庫ありのみ。 GET フォームなので未チェック時は instock 自体が送られない。 */}
          <label className="flex h-10 items-center gap-2 text-sm text-zinc-700 dark:text-zinc-200">
            <input
              type="checkbox"
              name="instock"
              value="1"
              defaultChecked={inStockOnly}
              className="h-4 w-4 rounded border-zinc-300 dark:border-zinc-600"
            />
            在庫ありのみ
          </label>
          <button
            type="submit"
            className="h-10 rounded-full border border-zinc-200 px-6 text-sm text-zinc-700 transition-colors hover:bg-zinc-100 dark:border-zinc-700 dark:text-zinc-200 dark:hover:bg-zinc-900"
          >
            検索
          </button>
        </form>

        <QueryParamSelect param="sort" value={sort} label="並び順" options={SORT_OPTIONS} />
      </div>

      {page.products.length === 0 ? (
        <p className="text-sm text-zinc-500 dark:text-zinc-400">
          条件に一致する商品がありません。
        </p>
      ) : (
        <div className="grid grid-cols-2 gap-5 sm:grid-cols-3 lg:grid-cols-4">
          {page.products.map((product) => (
            <div
              key={product.id}
              className="flex flex-col gap-3 rounded-2xl border border-zinc-200 bg-white p-4 dark:border-zinc-800 dark:bg-zinc-950"
            >
              <Link
                href={`/shop/products/${product.id}`}
                className="flex flex-col gap-3"
              >
                <div className="flex aspect-square items-center justify-center overflow-hidden rounded-xl bg-zinc-100 dark:bg-zinc-900">
                  {product.imageUrl ? (
                    <img
                      src={product.imageUrl}
                      alt={product.name}
                      className="h-full w-full object-cover"
                    />
                  ) : (
                    <span className="text-xs text-zinc-400 dark:text-zinc-600">画像なし</span>
                  )}
                </div>
                <div className="flex flex-col gap-1">
                  <h2 className="line-clamp-2 text-sm font-medium text-black hover:underline dark:text-zinc-50">
                    {product.name}
                  </h2>
                  <p className="text-base font-semibold text-black dark:text-zinc-50">
                    ¥{product.price.toLocaleString("ja-JP")}
                  </p>
                </div>
              </Link>
              <div className="mt-auto">
                <QuickAddButton productId={product.id} />
              </div>
            </div>
          ))}
        </div>
      )}

      <div className="flex flex-col items-center gap-3">
        <Pagination currentPage={page.pageNumber} totalPage={page.totalPage} />
        <p className="text-xs text-zinc-500 dark:text-zinc-400">
          全 {page.totalCount.toLocaleString("ja-JP")} 件
        </p>
      </div>
    </main>
  )
}
