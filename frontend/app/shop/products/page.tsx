/* eslint-disable @next/next/no-img-element */
import Link from "next/link"
import { Pagination } from "@/components/pagination"
import { QueryParamSelect } from "@/components/query-param-select"
import { Button } from "@/components/ui/button"
import { Card, CardContent } from "@/components/ui/card"
import { Checkbox } from "@/components/ui/checkbox"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
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
      <h1 className="text-2xl font-semibold">商品一覧</h1>

      {/* 左: 商品名検索（検索ボタンで適用）。 右: 並び順（変更で即適用）。 */}
      <div className="flex flex-wrap items-end justify-between gap-3">
        <form method="get" className="flex flex-wrap items-end gap-3">
          <div className="flex flex-col gap-1">
            <Label htmlFor="q" className="text-xs text-muted-foreground">
              商品名で検索
            </Label>
            <Input
              id="q"
              name="q"
              type="text"
              defaultValue={likeName}
              placeholder="部分一致"
              className="w-56"
            />
          </div>
          <input type="hidden" name="sort" value={sort} />
          {/* 在庫ありのみ。 GET フォームなので未チェック時は instock 自体が送られない。 */}
          <Label className="flex h-8 items-center gap-2 text-sm">
            <Checkbox name="instock" value="1" defaultChecked={inStockOnly} />
            在庫ありのみ
          </Label>
          <Button type="submit" variant="outline">
            検索
          </Button>
        </form>

        <QueryParamSelect param="sort" value={sort} label="並び順" options={SORT_OPTIONS} />
      </div>

      {page.products.length === 0 ? (
        <p className="text-sm text-muted-foreground">条件に一致する商品がありません。</p>
      ) : (
        <div className="grid grid-cols-2 gap-5 sm:grid-cols-3 lg:grid-cols-4">
          {page.products.map((product) => (
            <Card key={product.id} className="overflow-hidden py-0">
              <CardContent className="flex flex-col gap-3 p-4">
                <Link href={`/shop/products/${product.id}`} className="flex flex-col gap-3">
                  <div className="flex aspect-square items-center justify-center overflow-hidden rounded-xl bg-muted">
                    {product.imageUrl ? (
                      <img
                        src={product.imageUrl}
                        alt={product.name}
                        className="h-full w-full object-cover"
                      />
                    ) : (
                      <span className="text-xs text-muted-foreground">画像なし</span>
                    )}
                  </div>
                  <div className="flex flex-col gap-1">
                    <h2 className="line-clamp-2 text-sm font-medium hover:underline">
                      {product.name}
                    </h2>
                    <p className="text-base font-semibold">
                      ¥{product.price.toLocaleString("ja-JP")}
                    </p>
                  </div>
                </Link>
                <div className="mt-auto">
                  <QuickAddButton productId={product.id} />
                </div>
              </CardContent>
            </Card>
          ))}
        </div>
      )}

      <div className="flex flex-col items-center gap-3">
        <Pagination currentPage={page.pageNumber} totalPage={page.totalPage} />
        <p className="text-xs text-muted-foreground">
          全 {page.totalCount.toLocaleString("ja-JP")} 件
        </p>
      </div>
    </main>
  )
}
