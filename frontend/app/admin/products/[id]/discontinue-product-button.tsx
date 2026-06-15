"use client";

import { useState, useTransition } from "react";
import { Button } from "@/components/ui/button";
import { discontinueProduct } from "../actions";

export function DiscontinueProductButton({ id }: { id: string }) {
  const [confirming, setConfirming] = useState(false);
  const [isPending, startTransition] = useTransition();

  if (!confirming) {
    return (
      <Button
        type="button"
        variant="destructive"
        className="w-fit"
        onClick={() => setConfirming(true)}
      >
        この商品を生産終了にする
      </Button>
    );
  }

  return (
    <div className="flex flex-col gap-3">
      <p className="text-sm text-destructive">
        生産終了にすると販売・編集ができなくなります。 記録は残ります。
      </p>
      <div className="flex gap-3">
        <Button
          type="button"
          variant="destructive"
          disabled={isPending}
          onClick={() => startTransition(() => discontinueProduct(id))}
        >
          {isPending ? "処理中..." : "生産終了にする"}
        </Button>
        <Button
          type="button"
          variant="outline"
          disabled={isPending}
          onClick={() => setConfirming(false)}
        >
          キャンセル
        </Button>
      </div>
    </div>
  );
}
