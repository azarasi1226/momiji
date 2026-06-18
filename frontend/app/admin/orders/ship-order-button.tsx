"use client";

import { useTransition } from "react";
import { Button } from "@/components/ui/button";
import { shipOrder } from "./actions";

export function ShipOrderButton({ orderId }: { orderId: string }) {
  const [isPending, startTransition] = useTransition();

  return (
    <Button
      type="button"
      size="sm"
      disabled={isPending}
      onClick={() => startTransition(() => shipOrder(orderId))}
    >
      {isPending ? "発送処理中..." : "発送する"}
    </Button>
  );
}
