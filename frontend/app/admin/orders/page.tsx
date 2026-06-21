import type { Metadata } from "next";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { formatDateTime } from "@/lib/format";
import { listShippableOrders } from "./queries";
import { ShipOrderButton } from "./ship-order-button";

export const metadata: Metadata = {
  title: "発送管理",
};

export default async function OrderShippingPage() {
  const orders = await listShippableOrders();

  return (
    <main className="flex w-full max-w-6xl flex-col gap-8 px-8 py-16">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-semibold">発送管理</h1>
        <span className="text-sm text-muted-foreground">
          発送待ち {orders.length} 件
        </span>
      </div>

      {orders.length === 0 ? (
        <p className="text-sm text-muted-foreground">
          発送待ちの注文はありません。
        </p>
      ) : (
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>注文日時</TableHead>
              <TableHead>お届け先</TableHead>
              <TableHead>商品</TableHead>
              <TableHead className="text-right">合計</TableHead>
              <TableHead className="text-right" />
            </TableRow>
          </TableHeader>
          <TableBody>
            {orders.map((order) => (
              <TableRow key={order.orderId}>
                <TableCell className="text-muted-foreground">
                  {formatDateTime(order.createdAt)}
                </TableCell>
                <TableCell>
                  <div className="flex flex-col gap-0.5">
                    <span className="font-medium">
                      {order.shippingAddress.recipientName} 様
                    </span>
                    <span className="text-xs text-muted-foreground">
                      〒{order.shippingAddress.postalCode}{" "}
                      {order.shippingAddress.prefecture}
                      {order.shippingAddress.city}
                      {order.shippingAddress.streetAddress}
                      {order.shippingAddress.building
                        ? ` ${order.shippingAddress.building}`
                        : ""}
                    </span>
                    <span className="text-xs text-muted-foreground">
                      TEL: {order.shippingAddress.phoneNumber}
                    </span>
                    {order.shippingAddress.deliveryNote && (
                      <span className="text-xs text-muted-foreground">
                        備考: {order.shippingAddress.deliveryNote}
                      </span>
                    )}
                  </div>
                </TableCell>
                <TableCell className="text-muted-foreground">
                  <div className="flex flex-col gap-0.5">
                    {order.items.map((item) => (
                      <span key={`${order.orderId}-${item.name}`}>
                        {item.name} × {item.quantity}
                      </span>
                    ))}
                  </div>
                </TableCell>
                <TableCell className="text-right">
                  ¥{order.totalAmount.toLocaleString("ja-JP")}
                </TableCell>
                <TableCell className="text-right">
                  <ShipOrderButton orderId={order.orderId} />
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      )}
    </main>
  );
}
