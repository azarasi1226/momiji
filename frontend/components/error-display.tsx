"use client";

import { Button } from "@/components/ui/button";

export function ErrorDisplay({ reset }: { reset: () => void }) {
  return (
    <main className="flex min-h-[60vh] flex-col items-center justify-center gap-4 text-center">
      <h1 className="text-2xl font-semibold">問題が発生しました</h1>
      <p className="text-sm text-muted-foreground">
        予期しないエラーが発生しました。時間をおいて再度お試しください。
      </p>
      <Button type="button" variant="outline" onClick={reset}>
        再読み込み
      </Button>
    </main>
  );
}
