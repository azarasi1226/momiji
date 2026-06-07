import Link from "next/link"
import { findBasket } from "../actions"
import { BasketItemRow } from "./basket-item-row"
import { ClearBasketButton } from "./clear-basket-button"

// カゴの商品種類数は backend で最大 50 に制限される。 1 ページ（最大 100）で全件取れるため、
// カゴ画面はページングせず全件をまとめて表示し、合計金額を正確に出す。
const PAGE_SIZE = 100

export default async function BasketPage() {
  const basket = await findBasket({ pageSize: PAGE_SIZE, pageNumber: 1 })

  const total = basket.items.reduce(
    (sum, item) => sum + item.productPrice * item.itemQuantity,
    0,
  )

  return (
    <main className="flex w-full max-w-3xl flex-col gap-6 px-8 py-12">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-semibold text-black dark:text-zinc-50">買い物かご</h1>
        {basket.items.length > 0 && <ClearBasketButton />}
      </div>

      {basket.items.length === 0 ? (
        <div className="flex flex-col items-start gap-4">
          <p className="text-sm text-zinc-500 dark:text-zinc-400">
            カゴに商品がありません。
          </p>
          <Link
            href="/shop/products"
            className="flex h-10 items-center justify-center rounded-full bg-foreground px-6 text-sm text-background transition-colors hover:bg-[#383838] dark:hover:bg-[#ccc]"
          >
            商品一覧へ
          </Link>
        </div>
      ) : (
        <>
          <div className="flex flex-col rounded-2xl border border-zinc-200 bg-white px-5 dark:border-zinc-800 dark:bg-zinc-950">
            {basket.items.map((item) => (
              <BasketItemRow
                key={item.productId}
                productId={item.productId}
                productName={item.productName}
                productPrice={item.productPrice}
                productImageUrl={item.productImageUrl}
                itemQuantity={item.itemQuantity}
              />
            ))}
          </div>

          <div className="flex items-center justify-between border-t border-zinc-200 pt-4 dark:border-zinc-800">
            <span className="text-sm text-zinc-600 dark:text-zinc-400">
              {basket.totalCount.toLocaleString("ja-JP")} 種類
            </span>
            <span className="text-lg font-semibold text-black dark:text-zinc-50">
              合計 ¥{total.toLocaleString("ja-JP")}
            </span>
          </div>

          <Link
            href="/shop/products"
            className="text-sm text-zinc-500 transition-colors hover:text-black dark:text-zinc-400 dark:hover:text-zinc-50"
          >
            ← 買い物を続ける
          </Link>
        </>
      )}
    </main>
  )
}
