"use client";

import Link from "next/link";
import { useState, useTransition } from "react";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Label } from "@/components/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { setBasketItem } from "../../actions";

// 在庫が少ないとき「残りN点」を出す閾値（Amazon 風の煽り表示）。
const LOW_STOCK_THRESHOLD = 10;

export function AddToBasketPanel({
  productId,
  price,
  available,
}: {
  productId: string;
  price: number;
  available: number;
}) {
  const inStock = available > 0;
  const maxQuantity = Math.min(99, available);

  const [quantity, setQuantity] = useState(1);
  const [isPending, startTransition] = useTransition();
  const [message, setMessage] = useState<{ ok: boolean; text: string } | null>(
    null,
  );

  function handleAdd() {
    setMessage(null);
    startTransition(async () => {
      const result = await setBasketItem(productId, quantity);
      if (result?.error) {
        setMessage({ ok: false, text: result.error });
      } else {
        setMessage({ ok: true, text: `カートに入れました（${quantity}点）` });
      }
    });
  }

  return (
    <Card>
      <CardContent className="flex w-full flex-col gap-4">
        <p className="text-2xl font-bold">
          ¥{price.toLocaleString("ja-JP")}
          <span className="ml-1 text-sm font-normal text-muted-foreground">
            税込
          </span>
        </p>

        <StockStatus available={available} />

        {inStock ? (
          <>
            <div className="flex items-center gap-2">
              <Label
                htmlFor="quantity"
                className="text-sm text-muted-foreground"
              >
                数量
              </Label>
              <Select
                value={String(quantity)}
                onValueChange={(v) => setQuantity(Number(v))}
                disabled={isPending}
              >
                <SelectTrigger id="quantity" size="sm" className="w-20">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {Array.from({ length: maxQuantity }, (_, i) => i + 1).map(
                    (n) => (
                      <SelectItem key={n} value={String(n)}>
                        {n}
                      </SelectItem>
                    ),
                  )}
                </SelectContent>
              </Select>
            </div>

            <Button
              type="button"
              size="lg"
              onClick={handleAdd}
              disabled={isPending}
            >
              {isPending ? "追加中..." : "カートに入れる"}
            </Button>
          </>
        ) : (
          <Button type="button" size="lg" disabled>
            在庫切れ
          </Button>
        )}

        {message && (
          <div className="flex flex-col gap-1">
            <p
              className={
                message.ok
                  ? "text-xs text-green-600"
                  : "text-xs text-destructive"
              }
            >
              {message.text}
            </p>
            {message.ok && (
              <Link
                href="/shop/basket"
                className="text-xs text-primary hover:underline"
              >
                買い物かごを見る →
              </Link>
            )}
          </div>
        )}
      </CardContent>
    </Card>
  );
}

function StockStatus({ available }: { available: number }) {
  if (available <= 0) {
    return <p className="text-sm font-medium text-destructive">在庫切れ</p>;
  }
  if (available <= LOW_STOCK_THRESHOLD) {
    return (
      <p className="text-sm font-medium text-orange-600 dark:text-orange-400">
        残り{available.toLocaleString("ja-JP")}点 ご注文はお早めに
      </p>
    );
  }
  return (
    <p className="text-sm font-medium text-green-600 dark:text-green-400">
      在庫あり
    </p>
  );
}
