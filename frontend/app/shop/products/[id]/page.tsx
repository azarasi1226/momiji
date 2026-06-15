import Image from "next/image";
import Link from "next/link";
import { Card, CardContent } from "@/components/ui/card";
import { fetchShopProduct, fetchShopStock } from "../../actions";
import { AddToBasketPanel } from "./add-to-basket-panel";

export default async function ShopProductDetailPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = await params;
  const product = await fetchShopProduct(id);
  const stock = product.isActive ? await fetchShopStock(id) : { available: 0 };

  return (
    <main className="flex w-full max-w-5xl flex-col gap-6 px-8 py-12">
      <Link
        href="/shop/products"
        className="text-sm text-muted-foreground transition-colors hover:text-foreground"
      >
        ← 商品一覧
      </Link>

      <div className="grid gap-8 md:grid-cols-[1fr_360px]">
        {/* 左: 画像 + 商品情報 */}
        <div className="flex flex-col gap-6">
          <div className="relative aspect-square w-full overflow-hidden rounded-2xl bg-muted">
            {product.imageUrl ? (
              <Image
                src={product.imageUrl}
                alt={product.name}
                fill
                className="object-contain"
              />
            ) : (
              <span className="flex h-full w-full items-center justify-center text-sm text-muted-foreground">
                画像なし
              </span>
            )}
          </div>

          <div className="flex flex-col gap-3">
            <h1 className="text-2xl font-semibold">{product.name}</h1>
            <p className="whitespace-pre-wrap text-sm leading-relaxed text-muted-foreground">
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
            <Card>
              <CardContent className="flex flex-col gap-3">
                <p className="text-sm text-muted-foreground">
                  この商品は現在お取り扱いしていません。
                </p>
                <Link
                  href="/shop/products"
                  className="text-sm text-primary hover:underline"
                >
                  他の商品を見る →
                </Link>
              </CardContent>
            </Card>
          )}
        </div>
      </div>
    </main>
  );
}
