import { ListMyCardsService } from "@/grpc/gen/momiji/payment/listmycards/listmycards_pb.js";
import { createGrpcClient } from "@/lib/grpc";
import { redirectIfUnauthenticated } from "@/lib/grpc-error";
import { requireValidSession } from "@/lib/session";

export type Card = {
  id: string;
  brand: string;
  last4: string;
  expMonth: number;
  expYear: number;
  isDefault: boolean;
};

/** 保存カード一覧を取得する。 */
export async function fetchCards(): Promise<Card[]> {
  const session = await requireValidSession();
  try {
    const client = createGrpcClient(ListMyCardsService, session.accessToken);
    const response = await client.listMyCards({});
    return response.cards.map((card) => ({
      id: card.id,
      brand: card.brand,
      last4: card.last4,
      expMonth: card.expMonth,
      expYear: card.expYear,
      isDefault: card.isDefault,
    }));
  } catch (e) {
    redirectIfUnauthenticated(e);
    throw e;
  }
}
