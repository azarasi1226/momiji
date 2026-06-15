"use client";

import {
  Elements,
  PaymentElement,
  useElements,
  useStripe,
} from "@stripe/react-stripe-js";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { Button } from "@/components/ui/button";
import { getStripe } from "@/lib/stripe";
import { prepareCardRegistration } from "./actions";

/**
 * カード追加フォーム。
 *
 * 1. 「カードを追加」で backend に登録準備を要求し client_secret を得る
 * 2. Stripe Elements を表示し、 confirmSetup でカード入力 / 3DS を行う
 * 3. 成功すると backend が webhook でカードを記録する（非同期）。 少し待って一覧を更新する
 */
export function AddCardForm() {
  const [clientSecret, setClientSecret] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  async function start() {
    setLoading(true);
    setError(null);
    const result = await prepareCardRegistration();
    setLoading(false);
    if ("error" in result) {
      setError(result.error);
      return;
    }
    setClientSecret(result.clientSecret);
  }

  if (!clientSecret) {
    return (
      <div className="flex flex-col gap-2">
        <Button type="button" onClick={start} disabled={loading}>
          {loading ? "準備中..." : "カードを追加"}
        </Button>
        {error && <p className="text-sm text-destructive">{error}</p>}
      </div>
    );
  }

  return (
    <Elements stripe={getStripe()} options={{ clientSecret }}>
      <CardEntry onDone={() => setClientSecret(null)} />
    </Elements>
  );
}

function CardEntry({ onDone }: { onDone: () => void }) {
  const stripe = useStripe();
  const elements = useElements();
  const router = useRouter();
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function onSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!stripe || !elements) return;

    setSubmitting(true);
    setError(null);
    const { error: stripeError } = await stripe.confirmSetup({
      elements,
      confirmParams: { return_url: window.location.href },
      // 3DS が不要なら画面遷移せずその場で完了する（ローカルのテストカード向け）。
      redirect: "if_required",
    });
    setSubmitting(false);

    if (stripeError) {
      setError(stripeError.message ?? "カードの登録に失敗しました");
      return;
    }

    // 登録成功。 カード行は webhook 経由で非同期に作られるため、 少し待ってから一覧を再取得する。
    onDone();
    setTimeout(() => router.refresh(), 1500);
  }

  return (
    <form onSubmit={onSubmit} className="flex flex-col gap-4">
      {/* momiji は自前の保存カード機構を持つため、 Stripe Link（メール/電話でのカード保存・自動入力）は出さない */}
      <PaymentElement options={{ wallets: { link: "never" } }} />
      <div className="flex gap-2">
        <Button type="submit" disabled={!stripe || submitting}>
          {submitting ? "登録中..." : "このカードを登録"}
        </Button>
        <Button
          type="button"
          variant="outline"
          onClick={onDone}
          disabled={submitting}
        >
          キャンセル
        </Button>
      </div>
      {error && <p className="text-sm text-destructive">{error}</p>}
    </form>
  );
}
