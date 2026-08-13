"use client";

import { Minus, Plus, Trash2 } from "lucide-react";
import Image from "next/image";
import { useOptimistic, useState, useTransition } from "react";
import { Button } from "@/components/ui/button";
import { deleteBasketItem, setBasketItem } from "./actions";

// 個数の範囲（domain の BasketItemQuantity と一致。 0 は「カゴから消す」なので最小は 1）。
const MIN_QUANTITY = 1;
const MAX_QUANTITY = 99;

type Props = {
  productId: string;
  productName: string;
  productPrice: number;
  productImageUrl: string;
  itemQuantity: number;
};

/** カゴの1行。 - / + ボタンで個数を即時更新（絶対値 set）し、 削除もできる。 */
export function BasketItemRow({
  productId,
  productName,
  productPrice,
  productImageUrl,
  itemQuantity,
}: Props) {
  const [isPending, startTransition] = useTransition();
  const [error, setError] = useState<string | null>(null);
  // 楽観的更新: クリックで即 UI に反映し、 サーバー反映（revalidate）後は実値へ自動同期される。
  const [quantity, setOptimisticQuantity] = useOptimistic(itemQuantity);

  const subtotal = productPrice * quantity;

  // - / + で即更新。 押し忘れが起きないよう更新ボタンは廃止。
  function changeQuantity(next: number) {
    if (next < MIN_QUANTITY || next > MAX_QUANTITY) return;
    setError(null);
    startTransition(async () => {
      setOptimisticQuantity(next);
      const result = await setBasketItem(productId, next);
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
              sizes="64px"
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

        <div className="flex items-center gap-1">
          {quantity <= MIN_QUANTITY ? (
            // 残り1個で - を押すと削除（0 は「カゴから消す」なので、 減算でなく削除にする）。
            <Button
              type="button"
              variant="outline"
              size="icon-sm"
              className="text-destructive hover:text-destructive"
              aria-label="削除"
              onClick={handleRemove}
              disabled={isPending}
            >
              <Trash2 />
            </Button>
          ) : (
            <Button
              type="button"
              variant="outline"
              size="icon-sm"
              aria-label="1つ減らす"
              onClick={() => changeQuantity(quantity - 1)}
            >
              <Minus />
            </Button>
          )}
          <span
            className="w-8 text-center text-sm tabular-nums"
            aria-live="polite"
          >
            {quantity}
          </span>
          <Button
            type="button"
            variant="outline"
            size="icon-sm"
            aria-label="1つ増やす"
            onClick={() => changeQuantity(quantity + 1)}
            disabled={quantity >= MAX_QUANTITY}
          >
            <Plus />
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
