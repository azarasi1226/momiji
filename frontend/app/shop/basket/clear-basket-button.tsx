"use client";

import { useState, useTransition } from "react";
import { Button } from "@/components/ui/button";
import { clearBasket } from "./actions";

/** カゴを空にする。 誤操作防止に確認を挟む。 */
export function ClearBasketButton() {
  const [confirming, setConfirming] = useState(false);
  const [isPending, startTransition] = useTransition();
  const [error, setError] = useState<string | null>(null);

  function handleClear() {
    setError(null);
    startTransition(async () => {
      const result = await clearBasket();
      if (result?.error) {
        setError(result.error);
        setConfirming(false);
      }
    });
  }

  if (!confirming) {
    return (
      <Button
        type="button"
        variant="ghost"
        size="sm"
        className="text-destructive hover:text-destructive"
        onClick={() => setConfirming(true)}
      >
        カゴを空にする
      </Button>
    );
  }

  return (
    <div className="flex items-center gap-3">
      {error && <span className="text-xs text-destructive">{error}</span>}
      <span className="text-sm text-muted-foreground">
        本当に空にしますか？
      </span>
      <Button
        type="button"
        variant="destructive"
        size="sm"
        onClick={handleClear}
        disabled={isPending}
      >
        {isPending ? "処理中..." : "空にする"}
      </Button>
      <Button
        type="button"
        variant="outline"
        size="sm"
        onClick={() => setConfirming(false)}
        disabled={isPending}
      >
        キャンセル
      </Button>
    </div>
  );
}
