"use client";

import { useRouter } from "next/navigation";
import { useState, useTransition } from "react";
import { Button } from "@/components/ui/button";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { CancellationReason } from "@/grpc/gen/momiji/order/cancel/v1/cancel_pb.js";
import { cancelOrder } from "../actions";

// ユーザーが選べるキャンセル理由（UNSPECIFIED は選択肢に出さない）。
const REASON_OPTIONS: { value: CancellationReason; label: string }[] = [
  {
    value: CancellationReason.CHANGED_MIND,
    label: "気が変わった・不要になった",
  },
  { value: CancellationReason.ORDERED_BY_MISTAKE, label: "間違えて注文した" },
  {
    value: CancellationReason.FOUND_BETTER_PRICE,
    label: "他でより良い条件を見つけた",
  },
  { value: CancellationReason.DELIVERY_TOO_SLOW, label: "届くのが遅い" },
  { value: CancellationReason.OTHER, label: "その他" },
];

export function CancelOrderForm({ orderId }: { orderId: string }) {
  const router = useRouter();
  const [reason, setReason] = useState<string>("");
  const [error, setError] = useState<string | null>(null);
  const [done, setDone] = useState(false);
  const [isPending, startTransition] = useTransition();

  if (done) {
    return (
      <p className="text-sm text-muted-foreground">
        キャンセルを受け付けました。 反映まで少し時間がかかる場合があります。
      </p>
    );
  }

  function handleCancel() {
    setError(null);
    startTransition(async () => {
      const result = await cancelOrder(
        orderId,
        Number(reason) as CancellationReason,
      );
      if (result?.error) {
        setError(result.error);
        return;
      }
      setDone(true);
      // projection は非同期なので即時に反映されないことがあるが、 取れる最新状態に更新する。
      router.refresh();
    });
  }

  return (
    <div className="flex flex-col gap-3">
      <Select value={reason} onValueChange={setReason}>
        <SelectTrigger className="w-full sm:w-72">
          <SelectValue placeholder="キャンセル理由を選択" />
        </SelectTrigger>
        <SelectContent>
          {REASON_OPTIONS.map((opt) => (
            <SelectItem key={opt.value} value={String(opt.value)}>
              {opt.label}
            </SelectItem>
          ))}
        </SelectContent>
      </Select>

      {error && <p className="text-sm text-destructive">{error}</p>}

      <Button
        variant="destructive"
        onClick={handleCancel}
        disabled={isPending || !reason}
        className="w-fit"
      >
        {isPending ? "処理中..." : "注文をキャンセルする"}
      </Button>
    </div>
  );
}
