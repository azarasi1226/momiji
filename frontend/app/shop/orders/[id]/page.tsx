import type { Metadata } from "next";
import Image from "next/image";
import Link from "next/link";
import { notFound } from "next/navigation";
import {
  fetchMyOrder,
  isCancelable,
  type MyOrderDetail,
  ORDER_STATUS_LABEL,
} from "@/app/shop/orders/queries";
import { Badge } from "@/components/ui/badge";
import { Card, CardContent } from "@/components/ui/card";
import { Separator } from "@/components/ui/separator";
import { OrderStatus } from "@/grpc/gen/momiji/order/status_pb.js";
import { formatDateTime } from "@/lib/format";
import { CancelOrderForm } from "./cancel-order-form";

export const metadata: Metadata = {
  title: "注文詳細",
};

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

// 配達状況タイムラインの段（注文〜発送）。 status を「どこまで到達したか」のインデックスに対応づける。
// 「お届け（配達完了）」を表すステータスはまだ無いので、 発送までで止める。
const DELIVERY_STEPS = ["注文受付", "お支払い", "発送"] as const;

/** status が到達済みの最終段インデックス。 PAYMENT_PENDING は「注文受付」止まり（お支払い手続き中）。 FAILED は -1。 */
function reachedStepIndex(status: OrderStatus): number {
  switch (status) {
    case OrderStatus.STARTED:
    case OrderStatus.PAYMENT_PENDING:
      return 0;
    case OrderStatus.PAID:
      return 1;
    // SHIPPED / COMPLETED はどちらも「発送」段（最後）に丸める。
    case OrderStatus.SHIPPED:
    case OrderStatus.COMPLETED:
      return 2;
    default:
      return -1;
  }
}

function DeliveryTimeline({ status }: { status: OrderStatus }) {
  if (status === OrderStatus.CANCELLED) {
    return (
      <p className="text-sm text-destructive">
        この注文はキャンセルされました。
      </p>
    );
  }
  if (status === OrderStatus.FAILED) {
    return <p className="text-sm text-destructive">この注文は失敗しました。</p>;
  }
  const reached = reachedStepIndex(status);
  return (
    <ol className="flex flex-col gap-3">
      {DELIVERY_STEPS.map((label, i) => {
        const done = i <= reached;
        const current = i === reached;
        return (
          <li key={label} className="flex items-center gap-3">
            <span
              className={`flex size-5 items-center justify-center rounded-full border text-[10px] ${
                done
                  ? "border-primary bg-primary text-primary-foreground"
                  : "border-muted-foreground/40 text-muted-foreground"
              }`}
            >
              {done ? "✓" : i + 1}
            </span>
            <span
              className={
                current
                  ? "text-sm font-medium"
                  : done
                    ? "text-sm text-muted-foreground"
                    : "text-sm text-muted-foreground/60"
              }
            >
              {label}
              {status === OrderStatus.PAYMENT_PENDING && i === 1
                ? "（手続き中）"
                : ""}
            </span>
          </li>
        );
      })}
    </ol>
  );
}

function ShippingAddressBlock({
  address,
}: {
  address: NonNullable<MyOrderDetail["shippingAddress"]>;
}) {
  return (
    <div className="flex flex-col gap-0.5 text-sm">
      <span className="font-medium">{address.recipientName} 様</span>
      <span className="text-muted-foreground">〒{address.postalCode}</span>
      <span className="text-muted-foreground">
        {address.prefecture}
        {address.city}
        {address.streetAddress}
        {address.building ? ` ${address.building}` : ""}
      </span>
      <span className="text-muted-foreground">TEL: {address.phoneNumber}</span>
      {address.deliveryNote ? (
        <span className="text-muted-foreground">
          配達メモ: {address.deliveryNote}
        </span>
      ) : null}
    </div>
  );
}

export default async function MyOrderDetailPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = await params;
  const order = await fetchMyOrder(id);
  if (!order) notFound();

  return (
    <main className="flex w-full max-w-3xl flex-col gap-6 px-8 py-12">
      <div className="flex flex-wrap items-start justify-between gap-2">
        <div className="flex flex-col gap-1">
          <h1 className="text-2xl font-semibold">注文詳細</h1>
          <span className="text-xs text-muted-foreground">
            注文番号: <span className="font-mono">{order.orderId}</span>
          </span>
          <span className="text-xs text-muted-foreground">
            注文日時: {formatDateTime(order.createdAt)}
          </span>
        </div>
        <Badge variant={statusVariant(order.status)}>
          {ORDER_STATUS_LABEL[order.status]}
        </Badge>
      </div>

      <Card className="p-5">
        <CardContent className="flex flex-col gap-4 p-0 sm:flex-row sm:gap-8">
          <div className="flex flex-1 flex-col gap-2">
            <h2 className="text-sm font-medium">お届け先</h2>
            {order.shippingAddress ? (
              <ShippingAddressBlock address={order.shippingAddress} />
            ) : (
              <span className="text-sm text-muted-foreground">—</span>
            )}
          </div>
          <div className="flex flex-1 flex-col gap-2">
            <h2 className="text-sm font-medium">お支払い方法</h2>
            {order.paymentMethod ? (
              <span className="text-sm">
                {order.paymentMethod.brand} •••• {order.paymentMethod.last4}
              </span>
            ) : (
              <span className="text-sm text-muted-foreground">未設定</span>
            )}
          </div>
        </CardContent>
      </Card>

      <Card className="p-5">
        <CardContent className="flex flex-col gap-3 p-0">
          <h2 className="text-sm font-medium">配達状況</h2>
          <DeliveryTimeline status={order.status} />
        </CardContent>
      </Card>

      <Card className="p-5">
        <CardContent className="flex flex-col gap-4 p-0">
          <h2 className="text-sm font-medium">注文内容</h2>
          <div className="flex flex-col gap-3">
            {order.items.map((item) => (
              <div key={item.productId} className="flex items-center gap-3">
                <div className="relative size-14 shrink-0 overflow-hidden rounded-md bg-muted">
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
                    ¥{item.unitPrice.toLocaleString("ja-JP")} × {item.quantity}
                  </span>
                </div>
                <span className="text-sm font-medium">
                  ¥{(item.unitPrice * item.quantity).toLocaleString("ja-JP")}
                </span>
              </div>
            ))}
          </div>
          <Separator />
          <div className="flex items-center justify-between">
            <span className="text-sm text-muted-foreground">合計</span>
            <span className="text-lg font-semibold">
              ¥{order.totalAmount.toLocaleString("ja-JP")}
            </span>
          </div>
        </CardContent>
      </Card>

      {isCancelable(order.status) ? (
        <Card className="flex flex-col items-start gap-3 p-5">
          <h2 className="text-sm font-medium">注文のキャンセル</h2>
          <p className="text-xs text-muted-foreground">
            発送前の注文はキャンセルできます。
            お支払い済みの場合は返金されます。
          </p>
          <CancelOrderForm orderId={order.orderId} />
        </Card>
      ) : null}

      <Link
        href="/shop/orders"
        className="text-sm text-muted-foreground transition-colors hover:text-foreground"
      >
        ← 注文履歴に戻る
      </Link>
    </main>
  );
}
