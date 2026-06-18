"use client";

import { useState, useTransition } from "react";
import { Button } from "@/components/ui/button";
import { setBasketItem } from "../basket/actions";

/**
 * 一覧カード用の「カートに入れる」（数量 1 固定）。
 * 数量指定は商品詳細ページで行う方針なので、 ここはワンタップ追加だけ。
 */
export function QuickAddButton({ productId }: { productId: string }) {
  const [isPending, startTransition] = useTransition();
  const [message, setMessage] = useState<{ ok: boolean; text: string } | null>(
    null,
  );

  function handleAdd() {
    setMessage(null);
    startTransition(async () => {
      const result = await setBasketItem(productId, 1);
      setMessage(
        result?.error
          ? { ok: false, text: result.error }
          : { ok: true, text: "カートに入れました" },
      );
    });
  }

  return (
    <div className="flex flex-col gap-1">
      <Button
        type="button"
        onClick={handleAdd}
        disabled={isPending}
        className="w-full"
      >
        {isPending ? "追加中..." : "カートに入れる"}
      </Button>
      {message && (
        <p
          className={
            message.ok ? "text-xs text-green-600" : "text-xs text-destructive"
          }
        >
          {message.text}
        </p>
      )}
    </div>
  );
}
