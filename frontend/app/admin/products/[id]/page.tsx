import Link from "next/link"
import { productStatusLabel } from "@/lib/status-labels"
import { fetchBrand } from "../../brands/actions"
import { fetchProduct, fetchStock } from "../actions"
import { ProductEditForm } from "./product-edit-form"
import { DiscontinueProductButton } from "./discontinue-product-button"
import { StockForms } from "./stock-forms"

export default async function ProductDetailPage({
  params,
}: {
  params: Promise<{ id: string }>
}) {
  const { id } = await params
  const product = await fetchProduct(id)
  const [brand, stock] = await Promise.all([fetchBrand(product.brandId), fetchStock(id)])

  const discontinued = product.status === "DISCONTINUED"

  return (
    <main className="flex w-full max-w-2xl flex-col gap-8 px-8 py-16">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-semibold text-black dark:text-zinc-50">
          商品編集
        </h1>
        <Link
          href="/admin/products"
          className="text-sm text-zinc-500 hover:text-zinc-800 dark:text-zinc-400 dark:hover:text-zinc-100"
        >
          戻る
        </Link>
      </div>

      <p className="text-xs text-zinc-400 dark:text-zinc-500">
        ID: {product.id} ／ 状態: {productStatusLabel(product.status)}
      </p>

      {discontinued && (
        <p className="rounded-lg bg-zinc-100 px-4 py-3 text-sm text-zinc-600 dark:bg-zinc-900 dark:text-zinc-400">
          この商品は生産終了済みです。 編集はできません。
        </p>
      )}

      <ProductEditForm product={product} brandName={brand.name} />

      <hr className="w-full border-zinc-200 dark:border-zinc-700" />

      {/* 在庫 */}
      <section className="flex flex-col gap-4">
        <h2 className="text-lg font-semibold text-black dark:text-zinc-50">在庫</h2>
        <div className="grid grid-cols-3 gap-3">
          <StockStat label="物理在庫" value={stock.onHand} />
          <StockStat label="確保済み" value={stock.reserved} />
          <StockStat label="販売可能" value={stock.available} emphasize />
        </div>
        <StockForms productId={product.id} />
      </section>

      <hr className="w-full border-zinc-200 dark:border-zinc-700" />

      {discontinued ? (
        <p className="text-sm text-zinc-400 dark:text-zinc-500">
          生産終了済みのため操作はありません。
        </p>
      ) : (
        <DiscontinueProductButton id={product.id} />
      )}
    </main>
  )
}

function StockStat({
  label,
  value,
  emphasize = false,
}: {
  label: string
  value: number
  emphasize?: boolean
}) {
  return (
    <div className="flex flex-col gap-1 rounded-xl border border-zinc-200 px-4 py-3 dark:border-zinc-800">
      <span className="text-xs text-zinc-500 dark:text-zinc-400">{label}</span>
      <span
        className={
          emphasize
            ? "text-xl font-semibold text-black dark:text-zinc-50"
            : "text-xl font-medium text-zinc-700 dark:text-zinc-300"
        }
      >
        {value.toLocaleString("ja-JP")}
      </span>
    </div>
  )
}
