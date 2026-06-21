import { timestampDate } from "@bufbuild/protobuf/wkt";
import { ListShippableOrdersService } from "@/grpc/gen/momiji/order/listshippableorders/listshippableorders_pb.js";
import { createGrpcClient } from "@/lib/grpc";
import { redirectIfUnauthenticated } from "@/lib/grpc-error";
import { requireValidSession } from "@/lib/session";

export type ShippableOrder = {
  orderId: string;
  shippingAddress: {
    recipientName: string;
    phoneNumber: string;
    postalCode: string;
    prefecture: string;
    city: string;
    streetAddress: string;
    building: string;
    deliveryNote: string;
  };
  totalAmount: number;
  createdAt: string;
  items: { name: string; quantity: number }[];
};

export async function listShippableOrders(): Promise<ShippableOrder[]> {
  const session = await requireValidSession();
  try {
    const client = createGrpcClient(
      ListShippableOrdersService,
      session.accessToken,
    );
    const res = await client.listShippableOrders({});
    return res.orders.map((o) => ({
      orderId: o.orderId,
      shippingAddress: {
        recipientName: o.shippingAddress?.recipientName ?? "",
        phoneNumber: o.shippingAddress?.phoneNumber ?? "",
        postalCode: o.shippingAddress?.postalCode ?? "",
        prefecture: o.shippingAddress?.prefecture ?? "",
        city: o.shippingAddress?.city ?? "",
        streetAddress: o.shippingAddress?.streetAddress ?? "",
        building: o.shippingAddress?.building ?? "",
        deliveryNote: o.shippingAddress?.deliveryNote ?? "",
      },
      totalAmount: Number(o.totalAmount),
      createdAt: o.createdAt ? timestampDate(o.createdAt).toISOString() : "",
      items: o.items.map((i) => ({ name: i.name, quantity: i.quantity })),
    }));
  } catch (e) {
    redirectIfUnauthenticated(e);
    throw e;
  }
}
