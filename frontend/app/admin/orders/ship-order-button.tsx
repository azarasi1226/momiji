"use client";

import { useState, useTransition } from "react";
import { Button } from "@/components/ui/button";
import { shipOrder } from "./actions";

export function ShipOrderButton({ orderId }: { orderId: string }) {
  const [isPending, startTransition] = useTransition();
  const [error, setError] = useState<string | null>(null);

  function handleClick() {
    setError(null);
    startTransition(async () => {
      const result = await shipOrder(orderId);
      // 成功時は revalidatePath で一覧が再取得され、 この注文（SHIPPED）は発送待ちから消える。
      if (!result.success) {
        setError(result.error);
      }
    });
  }

  return (
    <div className="flex flex-col items-end gap-1">
      <Button
        type="button"
        size="sm"
        disabled={isPending}
        onClick={handleClick}
      >
        {isPending ? "発送処理中..." : "発送する"}
      </Button>
      {error && <p className="text-xs text-destructive">{error}</p>}
    </div>
  );
}
