"use client";

import Link from "next/link";
import { useState, useTransition } from "react";
import type { Card as PaymentCard } from "@/app/profile/payment-methods/actions";
import type { ShippingAddress } from "@/app/profile/shipping-addresses/actions";
import { clearBasket } from "@/app/shop/actions";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Separator } from "@/components/ui/separator";
import { getStripe } from "@/lib/stripe";
import { preparePayment, startOrder } from "./actions";

export type CheckoutItem = {
  productId: string;
  productName: string;
  productPrice: number;
  itemQuantity: number;
};

type Props = {
  items: CheckoutItem[];
  total: number;
  addresses: ShippingAddress[];
  cards: PaymentCard[];
};

function addressLabel(a: ShippingAddress): string {
  return `${a.name} 様 / ${a.prefecture}${a.city}${a.streetAddress}${a.building ? ` ${a.building}` : ""}`;
}

function cardLabel(c: PaymentCard): string {
  return `${c.brand} •••• ${c.last4}（${c.expMonth}/${c.expYear}）`;
}

/**
 * 注文手続き。 配送先・カードを選び、 注文確定で StartOrder → 決済準備 → Stripe.js confirm（3DS）まで通す。
 * 決済の確定（PAID）は webhook 経由（非同期）なので、 成功画面は「処理中」表記にする。
 */
export function CheckoutForm({ items, total, addresses, cards }: Props) {
  const defaultAddress = addresses.find((a) => a.isDefault) ?? addresses[0];
  const defaultCard = cards.find((c) => c.isDefault) ?? cards[0];
  const [addressId, setAddressId] = useState(defaultAddress?.id ?? "");
  const [cardId, setCardId] = useState(defaultCard?.id ?? "");
  const [isPending, startTransition] = useTransition();
  const [error, setError] = useState<string | null>(null);
  const [completedOrderId, setCompletedOrderId] = useState<string | null>(null);

  function handlePay() {
    setError(null);
    startTransition(async () => {
      // 1. 注文開始（在庫予約）。
      const started = await startOrder(addressId, total);
      if (!started || started.error || !started.orderId) {
        setError(started?.error ?? "注文の開始に失敗しました");
        return;
      }
      const orderId = started.orderId;

      // 2. 決済準備（PaymentIntent 作成）。
      const prepared = await preparePayment(orderId, cardId);
      if (!prepared || prepared.error || !prepared.clientSecret) {
        setError(prepared?.error ?? "決済の準備に失敗しました");
        return;
      }
      const clientSecret = prepared.clientSecret;

      // 3. Stripe.js で confirm（必要なら 3DS をブラウザで突破）。 pm はサーバ側で PaymentIntent に紐付け済み。
      const stripe = await getStripe();
      if (!stripe) {
        setError("決済の初期化に失敗しました");
        return;
      }
      const { error: stripeError } =
        await stripe.confirmCardPayment(clientSecret);
      if (stripeError) {
        // 決済失敗 ＝ 注文失敗（webhook が在庫を即解放）。 もう一度押すと新しい注文として最初からやり直す。
        setError(
          stripeError.message ?? "決済に失敗しました。 もう一度お試しください",
        );
        return;
      }

      // 成功（succeeded / processing）。 確定（PAID）は webhook 経由で非同期に反映される。
      // カゴのクリアは order と切り離したクライアント主導の後処理。 非クリティカル（カゴ残りは許容・
      // ユーザーが手で消せる）なので、 結果は握りつぶして成功表示を妨げない。
      await clearBasket().catch(() => {});

      setCompletedOrderId(orderId);
    });
  }

  if (completedOrderId) {
    return (
      <Card className="flex flex-col items-start gap-3 p-6">
        <p className="text-lg font-semibold">ご注文ありがとうございました</p>
        <p className="text-sm text-muted-foreground">
          注文番号: <span className="font-mono">{completedOrderId}</span>
        </p>
        <p className="text-sm text-muted-foreground">
          決済を処理しています。 確定すると注文状況に反映されます。
        </p>
        <Button asChild variant="outline" size="sm">
          <Link href="/shop/products">買い物を続ける</Link>
        </Button>
      </Card>
    );
  }

  if (addresses.length === 0) {
    return (
      <Card className="flex flex-col items-start gap-3 p-6">
        <p className="text-sm text-muted-foreground">
          配送先が登録されていません。 先に配送先を登録してください。
        </p>
        <Button asChild size="sm">
          <Link href="/profile/shipping-addresses">配送先を登録する</Link>
        </Button>
      </Card>
    );
  }

  if (cards.length === 0) {
    return (
      <Card className="flex flex-col items-start gap-3 p-6">
        <p className="text-sm text-muted-foreground">
          お支払い用のカードが登録されていません。
          先にカードを登録してください。
        </p>
        <Button asChild size="sm">
          <Link href="/profile/payment-methods">カードを登録する</Link>
        </Button>
      </Card>
    );
  }

  return (
    <div className="flex flex-col gap-6">
      <section className="flex flex-col gap-2">
        <h2 className="text-sm font-medium">お届け先</h2>
        <Select value={addressId} onValueChange={setAddressId}>
          <SelectTrigger className="w-full">
            <SelectValue placeholder="配送先を選択" />
          </SelectTrigger>
          <SelectContent>
            {addresses.map((a) => (
              <SelectItem key={a.id} value={a.id}>
                {addressLabel(a)}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
        <Link
          href="/profile/shipping-addresses"
          className="text-xs text-muted-foreground transition-colors hover:text-foreground"
        >
          配送先を追加・編集する
        </Link>
      </section>

      <section className="flex flex-col gap-2">
        <h2 className="text-sm font-medium">お支払い方法</h2>
        <Select value={cardId} onValueChange={setCardId}>
          <SelectTrigger className="w-full">
            <SelectValue placeholder="カードを選択" />
          </SelectTrigger>
          <SelectContent>
            {cards.map((c) => (
              <SelectItem key={c.id} value={c.id}>
                {cardLabel(c)}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
        <Link
          href="/profile/payment-methods"
          className="text-xs text-muted-foreground transition-colors hover:text-foreground"
        >
          カードを追加・編集する
        </Link>
      </section>

      <section className="flex flex-col gap-2">
        <h2 className="text-sm font-medium">注文内容</h2>
        <Card className="px-5 py-3">
          {items.map((item) => (
            <div
              key={item.productId}
              className="flex items-center justify-between py-2 text-sm"
            >
              <span>
                {item.productName}
                <span className="text-muted-foreground">
                  {" "}
                  × {item.itemQuantity}
                </span>
              </span>
              <span>
                ¥
                {(item.productPrice * item.itemQuantity).toLocaleString(
                  "ja-JP",
                )}
              </span>
            </div>
          ))}
        </Card>
      </section>

      <Separator />

      <div className="flex items-center justify-between">
        <span className="text-sm text-muted-foreground">合計</span>
        <span className="text-lg font-semibold">
          ¥{total.toLocaleString("ja-JP")}
        </span>
      </div>

      {error && <p className="text-sm text-destructive">{error}</p>}

      <Button
        onClick={handlePay}
        disabled={isPending || !addressId || !cardId}
        size="lg"
      >
        {isPending ? "処理中..." : "注文を確定する"}
      </Button>

      <Link
        href="/shop/basket"
        className="text-sm text-muted-foreground transition-colors hover:text-foreground"
      >
        ← 買い物かごに戻る
      </Link>
    </div>
  );
}
