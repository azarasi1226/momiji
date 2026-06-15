"use client";

import Image from "next/image";
import { useState, useTransition } from "react";
import { Button } from "@/components/ui/button";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { deleteBasketItem, setBasketItem } from "../actions";

type Props = {
  productId: string;
  productName: string;
  productPrice: number;
  productImageUrl: string;
  itemQuantity: number;
};

/** カゴの1行。 個数変更（絶対値 set）と削除を行う。 */
export function BasketItemRow({
  productId,
  productName,
  productPrice,
  productImageUrl,
  itemQuantity,
}: Props) {
  const [quantity, setQuantity] = useState(itemQuantity);
  const [isPending, startTransition] = useTransition();
  const [error, setError] = useState<string | null>(null);

  const dirty = quantity !== itemQuantity;
  const subtotal = productPrice * itemQuantity;

  function handleUpdate() {
    setError(null);
    startTransition(async () => {
      const result = await setBasketItem(productId, quantity);
      if (result?.error) setError(result.error);
    });
  }

  function handleRemove() {
    setError(null);
    startTransition(async () => {
      const result = await deleteBasketItem(productId);
      if (result?.error) setError(result.error);
    });
  }

  return (
    <div className="flex flex-col gap-2 border-b py-4 last:border-b-0">
      <div className="flex items-center gap-4">
        <div className="relative h-16 w-16 shrink-0 overflow-hidden rounded-lg bg-muted">
          {productImageUrl ? (
            <Image
              src={productImageUrl}
              alt={productName}
              fill
              className="object-cover"
            />
          ) : (
            <span className="flex h-full w-full items-center justify-center text-[10px] text-muted-foreground">
              画像なし
            </span>
          )}
        </div>

        <div className="min-w-0 flex-1">
          <p className="truncate text-sm font-medium">{productName}</p>
          <p className="text-xs text-muted-foreground">
            単価 ¥{productPrice.toLocaleString("ja-JP")}
          </p>
        </div>

        <div className="flex items-center gap-2">
          <Select
            value={String(quantity)}
            onValueChange={(v) => setQuantity(Number(v))}
            disabled={isPending}
          >
            <SelectTrigger size="sm" className="w-16" aria-label="個数">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              {Array.from({ length: 99 }, (_, i) => i + 1).map((n) => (
                <SelectItem key={n} value={String(n)}>
                  {n}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
          <Button
            type="button"
            variant="outline"
            size="sm"
            onClick={handleUpdate}
            disabled={isPending || !dirty}
          >
            更新
          </Button>
        </div>

        <p className="w-24 text-right text-sm font-semibold">
          ¥{subtotal.toLocaleString("ja-JP")}
        </p>

        <Button
          type="button"
          variant="ghost"
          size="sm"
          className="text-destructive hover:text-destructive"
          onClick={handleRemove}
          disabled={isPending}
        >
          削除
        </Button>
      </div>
      {error && <p className="text-xs text-destructive">{error}</p>}
    </div>
  );
}
