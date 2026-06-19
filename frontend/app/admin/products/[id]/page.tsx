import Link from "next/link";
import { fetchBrand } from "@/app/admin/brands/queries";
import { fetchProduct, fetchStock } from "@/app/admin/products/queries";
import { Badge } from "@/components/ui/badge";
import { Card, CardContent } from "@/components/ui/card";
import { Separator } from "@/components/ui/separator";
import { productStatusLabel } from "@/lib/status-labels";
import { DiscontinueProductButton } from "./discontinue-product-button";
import { ProductEditForm } from "./product-edit-form";
import { StockForms } from "./stock-forms";

export default async function ProductDetailPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = await params;
  const product = await fetchProduct(id);
  const [brand, stock] = await Promise.all([
    fetchBrand(product.brandId),
    fetchStock(id),
  ]);

  const discontinued = product.status === "DISCONTINUED";

  return (
    <main className="flex w-full max-w-2xl flex-col gap-8 px-8 py-16">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-semibold">商品編集</h1>
        <Link
          href="/admin/products"
          className="text-sm text-muted-foreground transition-colors hover:text-foreground"
        >
          戻る
        </Link>
      </div>

      <div className="flex items-center gap-3 text-xs text-muted-foreground">
        <span>ID: {product.id}</span>
        <Badge variant={discontinued ? "secondary" : "default"}>
          {productStatusLabel(product.status)}
        </Badge>
      </div>

      {discontinued && (
        <p className="rounded-lg bg-muted px-4 py-3 text-sm text-muted-foreground">
          この商品は生産終了済みです。 編集はできません。
        </p>
      )}

      <ProductEditForm product={product} brandName={brand.name} />

      <Separator />

      {/* 在庫 */}
      <section className="flex flex-col gap-4">
        <h2 className="text-lg font-semibold">在庫</h2>
        <div className="grid grid-cols-3 gap-3">
          <StockStat label="物理在庫" value={stock.onHand} />
          <StockStat label="確保済み" value={stock.reserved} />
          <StockStat label="販売可能" value={stock.available} emphasize />
        </div>
        <StockForms productId={product.id} />
      </section>

      <Separator />

      {discontinued ? (
        <p className="text-sm text-muted-foreground">
          生産終了済みのため操作はありません。
        </p>
      ) : (
        <DiscontinueProductButton id={product.id} />
      )}
    </main>
  );
}

function StockStat({
  label,
  value,
  emphasize = false,
}: {
  label: string;
  value: number;
  emphasize?: boolean;
}) {
  return (
    <Card className="py-0">
      <CardContent className="flex flex-col gap-1 px-4 py-3">
        <span className="text-xs text-muted-foreground">{label}</span>
        <span
          className={
            emphasize ? "text-xl font-semibold" : "text-xl font-medium"
          }
        >
          {value.toLocaleString("ja-JP")}
        </span>
      </CardContent>
    </Card>
  );
}
