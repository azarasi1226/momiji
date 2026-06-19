import type { Metadata } from "next";
import Image from "next/image";
import Link from "next/link";
import { Pagination } from "@/components/pagination";
import { QueryParamSelect } from "@/components/query-param-select";
import { Badge } from "@/components/ui/badge";
import { Card, CardContent } from "@/components/ui/card";
import { Separator } from "@/components/ui/separator";
import { OrderStatus } from "@/grpc/gen/momiji/order/status_pb.js";
import { formatDateTime } from "@/lib/format";
import { listMyOrders, ORDER_STATUS_LABEL } from "./queries";

export const metadata: Metadata = {
  title: "注文履歴",
};

const PAGE_SIZE = 20;

// 暦年バケットを今年から何年分さかのぼって出すか。
const YEAR_BUCKETS = 5;

type DateRange = { from?: Date; to?: Date };

/**
 * URL の range クエリを具体的な日時区間 `[from, to)` に解決する。
 * - `1m` / `3m`: 現在を起点に遡る累積ウィンドウ（to は開放）。
 * - 4 桁の年: その暦年だけ（`[Y-01-01, (Y+1)-01-01)` を **JST(+09:00)** で切る）。
 * - `""` / `all` / 不明: 全期間（絞り込みなし）。
 */
function resolveRange(range: string, now: Date): DateRange {
  if (range === "1m") {
    const from = new Date(now);
    from.setMonth(from.getMonth() - 1);
    return { from };
  }
  if (range === "3m") {
    const from = new Date(now);
    from.setMonth(from.getMonth() - 3);
    return { from };
  }
  if (/^\d{4}$/.test(range)) {
    const year = Number(range);
    return {
      from: new Date(`${year}-01-01T00:00:00+09:00`),
      to: new Date(`${year + 1}-01-01T00:00:00+09:00`),
    };
  }
  return {};
}

/** ステータスごとの Badge の見た目。 完了系は控えめ、 失敗系は警告色。 */
function statusVariant(
  status: OrderStatus,
): "default" | "secondary" | "destructive" | "outline" {
  switch (status) {
    case OrderStatus.FAILED:
    case OrderStatus.CANCELLED:
      return "destructive";
    case OrderStatus.SHIPPED:
    case OrderStatus.COMPLETED:
      return "secondary";
    case OrderStatus.PAID:
      return "default";
    default:
      return "outline";
  }
}

export default async function MyOrdersPage({
  searchParams,
}: {
  searchParams: Promise<{ page?: string; range?: string }>;
}) {
  const sp = await searchParams;
  const pageNumber = Math.max(1, Number(sp.page ?? "1") || 1);
  const range = sp.range ?? "";

  const now = new Date();
  const { from, to } = resolveRange(range, now);
  const page = await listMyOrders({
    from,
    to,
    pageSize: PAGE_SIZE,
    pageNumber,
  });

  // 期間セレクトの選択肢。 年バケットは実行時の現在年から動的に算出する（今年〜N年前）。
  const currentYear = now.getFullYear();
  const rangeOptions = [
    { value: "1m", label: "直近1か月" },
    { value: "3m", label: "過去3か月" },
    { value: "", label: "すべて" },
    ...Array.from({ length: YEAR_BUCKETS }, (_, i) => {
      const year = currentYear - i;
      return { value: String(year), label: `${year}年` };
    }),
  ];

  return (
    <main className="flex w-full max-w-3xl flex-col gap-6 px-8 py-12">
      <div className="flex flex-wrap items-end justify-between gap-3">
        <h1 className="text-2xl font-semibold">注文履歴</h1>
        <QueryParamSelect
          param="range"
          value={range}
          label="期間"
          options={rangeOptions}
        />
      </div>

      {page.orders.length === 0 ? (
        <Card className="flex flex-col items-start gap-3 p-6">
          <p className="text-sm text-muted-foreground">
            {range ? "この期間の注文はありません。" : "まだ注文がありません。"}
          </p>
          {range ? null : (
            <Link
              href="/shop/products"
              className="text-sm text-primary underline-offset-4 hover:underline"
            >
              買い物を始める
            </Link>
          )}
        </Card>
      ) : (
        <div className="flex flex-col gap-4">
          {page.orders.map((order) => (
            <Link
              key={order.orderId}
              href={`/shop/orders/${order.orderId}`}
              className="rounded-xl transition-colors hover:bg-muted/40"
            >
              <Card className="p-5">
                <CardContent className="flex flex-col gap-4 p-0">
                  <div className="flex flex-wrap items-start justify-between gap-2">
                    <div className="flex flex-col gap-1">
                      <span className="text-xs text-muted-foreground">
                        注文番号:{" "}
                        <span className="font-mono">{order.orderId}</span>
                      </span>
                      <span className="text-xs text-muted-foreground">
                        {formatDateTime(order.createdAt)}
                      </span>
                    </div>
                    <Badge variant={statusVariant(order.status)}>
                      {ORDER_STATUS_LABEL[order.status]}
                    </Badge>
                  </div>

                  <Separator />

                  <div className="flex flex-col gap-3">
                    {order.items.map((item) => (
                      <div
                        key={item.productId}
                        className="flex items-center gap-3"
                      >
                        <div className="relative size-12 shrink-0 overflow-hidden rounded-md bg-muted">
                          {item.imageUrl ? (
                            <Image
                              src={item.imageUrl}
                              alt={item.name}
                              fill
                              className="object-cover"
                            />
                          ) : null}
                        </div>
                        <div className="flex flex-1 flex-col">
                          <span className="text-sm">{item.name}</span>
                          <span className="text-xs text-muted-foreground">
                            ¥{item.unitPrice.toLocaleString("ja-JP")} ×{" "}
                            {item.quantity}
                          </span>
                        </div>
                      </div>
                    ))}
                  </div>

                  <Separator />

                  <div className="flex items-center justify-between">
                    <span className="text-sm text-muted-foreground">合計</span>
                    <span className="text-base font-semibold">
                      ¥{order.totalAmount.toLocaleString("ja-JP")}
                    </span>
                  </div>
                </CardContent>
              </Card>
            </Link>
          ))}
        </div>
      )}

      <div className="flex flex-col items-center gap-3">
        <Pagination currentPage={page.pageNumber} totalPage={page.totalPage} />
        {page.orders.length > 0 ? (
          <p className="text-xs text-muted-foreground">
            全 {page.totalCount.toLocaleString("ja-JP")} 件
          </p>
        ) : null}
      </div>
    </main>
  );
}
