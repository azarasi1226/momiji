/* eslint-disable @next/next/no-img-element */
import Link from "next/link"
import { fetchShopProduct, fetchShopStock } from "../../actions"
import { AddToBasketPanel } from "./add-to-basket-panel"

export default async function ShopProductDetailPage({
  params,
}: {
  params: Promise<{ id: string }>
}) {
  const { id } = await params
  const product = await fetchShopProduct(id)
  const stock = product.isActive ? await fetchShopStock(id) : { available: 0 }

  return (
    <main className="flex w-full max-w-5xl flex-col gap-6 px-8 py-12">
      <Link
        href="/shop/products"
        className="text-sm text-zinc-500 transition-colors hover:text-black dark:text-zinc-400 dark:hover:text-zinc-50"
      >
        ← 商品一覧
      </Link>

      <div className="grid gap-8 md:grid-cols-[1fr_360px]">
        {/* 左: 画像 + 商品情報 */}
        <div className="flex flex-col gap-6">
          <div className="flex aspect-square w-full items-center justify-center overflow-hidden rounded-2xl bg-zinc-100 dark:bg-zinc-900">
            {product.imageUrl ? (
              <img
                src={product.imageUrl}
                alt={product.name}
                className="h-full w-full object-contain"
              />
            ) : (
              <span className="text-sm text-zinc-400 dark:text-zinc-600">画像なし</span>
            )}
          </div>

          <div className="flex flex-col gap-3">
            <h1 className="text-2xl font-semibold text-black dark:text-zinc-50">{product.name}</h1>
            <p className="whitespace-pre-wrap text-sm leading-relaxed text-zinc-600 dark:text-zinc-400">
              {product.description}
            </p>
          </div>
        </div>

        {/* 右: 購入パネル（在庫状況 + 数量 + カート） */}
        <div className="md:sticky md:top-24 md:self-start">
          {product.isActive ? (
            <AddToBasketPanel
              productId={product.id}
              price={product.price}
              available={stock.available}
            />
          ) : (
            <div className="flex flex-col gap-3 rounded-2xl border border-zinc-200 p-5 dark:border-zinc-800">
              <p className="text-sm text-zinc-500 dark:text-zinc-400">
                この商品は現在お取り扱いしていません。
              </p>
              <Link
                href="/shop/products"
                className="text-sm text-blue-600 hover:underline dark:text-blue-400"
              >
                他の商品を見る →
              </Link>
            </div>
          )}
        </div>
      </div>
    </main>
  )
}
