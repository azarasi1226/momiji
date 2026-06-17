import Link from "next/link";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Separator } from "@/components/ui/separator";
import { findBasket } from "../queries";
import { BasketItemRow } from "./basket-item-row";
import { ClearBasketButton } from "./clear-basket-button";

// カゴの商品種類数は backend で最大 50 に制限される。 1 ページ（最大 100）で全件取れるため、
// カゴ画面はページングせず全件をまとめて表示し、合計金額を正確に出す。
const PAGE_SIZE = 100;

export default async function BasketPage() {
  const basket = await findBasket({ pageSize: PAGE_SIZE, pageNumber: 1 });

  const total = basket.items.reduce(
    (sum, item) => sum + item.productPrice * item.itemQuantity,
    0,
  );

  return (
    <main className="flex w-full max-w-3xl flex-col gap-6 px-8 py-12">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-semibold">買い物かご</h1>
        {basket.items.length > 0 && <ClearBasketButton />}
      </div>

      {basket.items.length === 0 ? (
        <div className="flex flex-col items-start gap-4">
          <p className="text-sm text-muted-foreground">
            カゴに商品がありません。
          </p>
          <Button asChild>
            <Link href="/shop/products">商品一覧へ</Link>
          </Button>
        </div>
      ) : (
        <>
          <Card className="px-5 py-0">
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
          </Card>

          <Separator />

          <div className="flex items-center justify-between">
            <span className="text-sm text-muted-foreground">
              {basket.totalCount.toLocaleString("ja-JP")} 種類
            </span>
            <span className="text-lg font-semibold">
              合計 ¥{total.toLocaleString("ja-JP")}
            </span>
          </div>

          <Button asChild size="lg">
            <Link href="/shop/checkout">レジに進む</Link>
          </Button>

          <Link
            href="/shop/products"
            className="text-sm text-muted-foreground transition-colors hover:text-foreground"
          >
            ← 買い物を続ける
          </Link>
        </>
      )}
    </main>
  );
}
