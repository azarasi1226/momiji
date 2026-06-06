import Link from "next/link"
import { fetchBrand } from "../../brands/actions"
import { fetchProduct } from "../actions"
import { ProductEditForm } from "./product-edit-form"
import { DiscontinueProductButton } from "./discontinue-product-button"

export default async function ProductDetailPage({
  params,
}: {
  params: Promise<{ id: string }>
}) {
  const { id } = await params
  const product = await fetchProduct(id)
  const brand = await fetchBrand(product.brandId)

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
        ID: {product.id} ／ 状態: {product.status}
      </p>

      {discontinued && (
        <p className="rounded-lg bg-zinc-100 px-4 py-3 text-sm text-zinc-600 dark:bg-zinc-900 dark:text-zinc-400">
          この商品は生産終了済みです。 編集はできません。
        </p>
      )}

      <ProductEditForm product={product} brandName={brand.name} />

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
