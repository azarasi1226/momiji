import { timestampDate, timestampFromDate } from "@bufbuild/protobuf/wkt";
import { FindMyOrderService } from "@/grpc/gen/momiji/order/findmyorder/findmyorder_pb.js";
import { ListMyOrdersService } from "@/grpc/gen/momiji/order/listmyorders/listmyorders_pb.js";
import { OrderStatus } from "@/grpc/gen/momiji/order/status_pb.js";
import { createGrpcClient } from "@/lib/grpc";
import { notFound } from "next/navigation";
import { parseConnectError, redirectIfUnauthenticated } from "@/lib/grpc-error";
import { requireValidSession } from "@/lib/session";

export type MyOrderItem = {
  productId: string;
  name: string;
  unitPrice: number;
  quantity: number;
  imageUrl: string;
};

export type MyOrder = {
  orderId: string;
  status: OrderStatus;
  totalAmount: number;
  // UTC 絶対時刻の ISO 文字列。 表示時刻への変換は lib/format の formatDateTime に委ねる。
  createdAt: string;
  items: MyOrderItem[];
};

export type MyOrdersPage = {
  orders: MyOrder[];
  totalCount: number;
  totalPage: number;
  pageNumber: number;
};

/**
 * 自分の注文一覧を新しい順で取得する（持ち主は JWT から解決）。
 * [from]/[to] は注文日時の絞り込み（半開区間 `[from, to)`）。 期間の意味づけ（「直近1か月」「2025年」等）は
 * 呼び出し側が具体的な日時に解決して渡す。 未指定の側は開放される。
 */
export async function listMyOrders(params: {
  from?: Date;
  to?: Date;
  pageSize?: number;
  pageNumber?: number;
}): Promise<MyOrdersPage> {
  const session = await requireValidSession();
  try {
    const client = createGrpcClient(ListMyOrdersService, session.accessToken);
    const res = await client.listMyOrders({
      pageSize: params.pageSize ?? 0,
      pageNumber: params.pageNumber ?? 0,
      // optional フィールドは undefined で省略 → backend 側は範囲をその側で開放する。
      createdFrom: params.from ? timestampFromDate(params.from) : undefined,
      createdTo: params.to ? timestampFromDate(params.to) : undefined,
    });
    return {
      orders: res.orders.map((o) => ({
        orderId: o.orderId,
        status: o.status,
        // proto int64 → bigint。 円なので Number に落として安全。
        totalAmount: Number(o.totalAmount),
        createdAt: o.createdAt ? timestampDate(o.createdAt).toISOString() : "",
        items: o.items.map((i) => ({
          productId: i.productId,
          name: i.name,
          unitPrice: i.unitPrice,
          quantity: i.quantity,
          imageUrl: i.imageUrl ?? "",
        })),
      })),
      totalCount: Number(res.paging?.totalCount ?? 0),
      totalPage: res.paging?.totalPage ?? 0,
      pageNumber: res.paging?.pageNumber ?? 0,
    };
  } catch (e) {
    redirectIfUnauthenticated(e);
    throw e;
  }
}

export type MyOrderDetailItem = {
  productId: string;
  name: string;
  unitPrice: number;
  quantity: number;
  imageUrl: string;
};

export type MyOrderShippingAddress = {
  recipientName: string;
  phoneNumber: string;
  postalCode: string;
  prefecture: string;
  city: string;
  streetAddress: string;
  building: string;
  deliveryNote: string;
};

export type MyOrderPaymentMethod = {
  id: string;
  brand: string;
  last4: string;
};

export type MyOrderDetail = {
  orderId: string;
  status: OrderStatus;
  totalAmount: number;
  createdAt: string;
  updatedAt: string;
  shippingAddress: MyOrderShippingAddress | null;
  paymentMethod: MyOrderPaymentMethod | null;
  items: MyOrderDetailItem[];
};

export async function fetchMyOrder(orderId: string): Promise<MyOrderDetail> {
  const session = await requireValidSession();
  try {
    const client = createGrpcClient(FindMyOrderService, session.accessToken);
    const o = await client.findMyOrder({ orderId });
    const a = o.shippingAddress;
    const pm = o.paymentMethod;
    return {
      orderId: o.orderId,
      status: o.status,
      totalAmount: Number(o.totalAmount),
      createdAt: o.createdAt ? timestampDate(o.createdAt).toISOString() : "",
      updatedAt: o.updatedAt ? timestampDate(o.updatedAt).toISOString() : "",
      shippingAddress: a
        ? {
            recipientName: a.recipientName,
            phoneNumber: a.phoneNumber,
            postalCode: a.postalCode,
            prefecture: a.prefecture,
            city: a.city,
            streetAddress: a.streetAddress,
            building: a.building,
            deliveryNote: a.deliveryNote,
          }
        : null,
      paymentMethod: pm
        ? { id: pm.id, brand: pm.brand, last4: pm.last4 }
        : null,
      items: o.items.map((i) => ({
        productId: i.productId,
        name: i.name,
        unitPrice: i.unitPrice,
        quantity: i.quantity,
        imageUrl: i.imageUrl ?? "",
      })),
    };
  } catch (e) {
    redirectIfUnauthenticated(e);
    if (parseConnectError(e)?.businessError) notFound();
    throw e;
  }
}

/** 発送前（キャンセル可能とみなす）ステータスか。 SHIPPED 以降・FAILED は不可。 */
export function isCancelable(status: OrderStatus): boolean {
  return (
    status === OrderStatus.STARTED ||
    status === OrderStatus.PAYMENT_PENDING ||
    status === OrderStatus.PAID
  );
}

/** 注文ステータスの日本語ラベル。 */
export const ORDER_STATUS_LABEL: Record<OrderStatus, string> = {
  [OrderStatus.UNSPECIFIED]: "不明",
  [OrderStatus.STARTED]: "注文受付",
  [OrderStatus.PAYMENT_PENDING]: "お支払い手続き中",
  [OrderStatus.PAID]: "お支払い完了",
  [OrderStatus.SHIPPED]: "発送済み",
  // COMPLETED（在庫確定済み）も購入者目線では「発送済み」までで十分なので同じ表示に揃える。
  [OrderStatus.COMPLETED]: "発送済み",
  [OrderStatus.FAILED]: "失敗",
  [OrderStatus.CANCELLED]: "キャンセル済み",
};
