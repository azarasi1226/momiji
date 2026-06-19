import { ListMyShippingAddressesService } from "@/grpc/gen/momiji/user/shippingaddress/listmyshippingaddresses/listmyshippingaddresses_pb.js";
import { createGrpcClient } from "@/lib/grpc";
import { redirectIfUnauthenticated } from "@/lib/grpc-error";
import { requireValidSession } from "@/lib/session";

export type ShippingAddress = {
  id: string;
  name: string;
  phoneNumber: string;
  postalCode: string;
  prefecture: string;
  city: string;
  streetAddress: string;
  building: string;
  deliveryNote: string;
  isDefault: boolean;
};

/** 配送先一覧を取得する（登録順）。 */
export async function fetchShippingAddresses(): Promise<ShippingAddress[]> {
  const session = await requireValidSession();
  try {
    const client = createGrpcClient(
      ListMyShippingAddressesService,
      session.accessToken,
    );
    const response = await client.listMyShippingAddresses({});
    return response.shippingAddresses.map((address) => ({
      id: address.id,
      name: address.name,
      phoneNumber: address.phoneNumber,
      postalCode: address.postalCode,
      prefecture: address.prefecture,
      city: address.city,
      streetAddress: address.streetAddress,
      building: address.building,
      deliveryNote: address.deliveryNote,
      isDefault: address.isDefault,
    }));
  } catch (e) {
    redirectIfUnauthenticated(e);
    throw e;
  }
}
