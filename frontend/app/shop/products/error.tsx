"use client";

import { Button } from "@/components/ui/button";

export default function ShopProductsError({
  reset,
}: {
  error: Error & { digest?: string };
  reset: () => void;
}) {
  return (
    <main className="flex w-full max-w-5xl flex-col gap-6 px-8 py-12">
      <h1 className="text-2xl font-semibold">商品一覧</h1>
      <div className="flex flex-col gap-4">
        <p className="text-sm text-destructive">
          商品の読み込みに失敗しました。時間をおいて再度お試しください。
        </p>
        <Button
          type="button"
          variant="outline"
          onClick={reset}
          className="w-fit"
        >
          再読み込み
        </Button>
      </div>
    </main>
  );
}
