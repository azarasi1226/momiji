import Link from "next/link";
import { fetchCards } from "@/app/profile/payment-methods/queries";
import { fetchShippingAddresses } from "@/app/profile/shipping-addresses/queries";
import { findBasket } from "@/app/shop/basket/queries";
import { Button } from "@/components/ui/button";
import { CheckoutForm, type CheckoutItem } from "./checkout-form";

// カゴは最大 50 種類なので 1 ページ（最大 100）で全件取れる。 合計を正確に出すため全件取得。
const PAGE_SIZE = 100;

export default async function CheckoutPage() {
  const [basket, addresses, cards] = await Promise.all([
    findBasket({ pageSize: PAGE_SIZE, pageNumber: 1 }),
    fetchShippingAddresses(),
    fetchCards(),
  ]);

  const total = basket.items.reduce(
    (sum, item) => sum + item.productPrice * item.itemQuantity,
    0,
  );

  const items: CheckoutItem[] = basket.items.map((i) => ({
    productId: i.productId,
    productName: i.productName,
    productPrice: i.productPrice,
    itemQuantity: i.itemQuantity,
  }));

  return (
    <main className="flex w-full max-w-3xl flex-col gap-6 px-8 py-12">
      <h1 className="text-2xl font-semibold">ご注文手続き</h1>

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
        <CheckoutForm
          items={items}
          total={total}
          addresses={addresses}
          cards={cards}
        />
      )}
    </main>
  );
}
