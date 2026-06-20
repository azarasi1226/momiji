"use client";

import { ErrorDisplay } from "@/components/error-display";

export default function GlobalError({
  reset,
}: {
  error: Error & { digest?: string };
  reset: () => void;
}) {
  return (
    <html lang="ja">
      <body>
        <ErrorDisplay reset={reset} />
      </body>
    </html>
  );
}
