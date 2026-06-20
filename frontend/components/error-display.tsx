"use client";

import Link from "next/link";
import { Button } from "@/components/ui/button";

export function ErrorDisplay({ reset }: { reset: () => void }) {
  return (
    <main className="flex min-h-[60vh] flex-col items-center justify-center gap-4 text-center">
      <h1 className="text-2xl font-semibold">問題が発生しました</h1>
      <p className="text-sm text-muted-foreground">
        予期しないエラーが発生しました。時間をおいて再度お試しください。
      </p>
      <div className="flex gap-2">
        <Button type="button" variant="outline" onClick={reset}>
          再読み込み
        </Button>
        <Button asChild variant="outline">
          <Link href="/">ホームに戻る</Link>
        </Button>
      </div>
    </main>
  );
}
