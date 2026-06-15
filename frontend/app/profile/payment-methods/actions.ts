"use server";

import { revalidatePath } from "next/cache";
import { ChangeDefaultCardService } from "@/grpc/gen/momiji/payment/changedefaultcard/v1/changedefault_pb.js";
import { DeleteCardService } from "@/grpc/gen/momiji/payment/deletecard/v1/delete_pb.js";
import { ListCardsService } from "@/grpc/gen/momiji/payment/listcards/v1/list_pb.js";
import { PrepareCardRegistrationService } from "@/grpc/gen/momiji/payment/preparecard/v1/prepare_pb.js";
import { createGrpcClient } from "@/lib/grpc";
import { parseConnectError, redirectIfUnauthenticated } from "@/lib/grpc-error";
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
    const client = createGrpcClient(ListCardsService, session.accessToken);
    const response = await client.listCards({});
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

export type PrepareCardState = { clientSecret: string } | { error: string };

/**
 * カード登録準備。 backend が Stripe Customer を lazy 作成し SetupIntent を作って client_secret を返す。
 * フロントはこの client_secret を Stripe.js の confirmSetup に渡す。
 */
export async function prepareCardRegistration(): Promise<PrepareCardState> {
  const session = await requireValidSession();
  try {
    const client = createGrpcClient(
      PrepareCardRegistrationService,
      session.accessToken,
    );
    const response = await client.prepareCardRegistration({});
    return { clientSecret: response.clientSecret };
  } catch (e) {
    redirectIfUnauthenticated(e);
    const parsed = parseConnectError(e);
    if (parsed?.businessError) return { error: parsed.businessError };
    if (parsed?.unknownError) {
      return {
        error: `${parsed.unknownError.message} (問い合わせ番号: ${parsed.unknownError.correlationId})`,
      };
    }
    return { error: "カード登録の準備に失敗しました" };
  }
}

export type CardActionState = { error?: string } | null;

/** カードを削除する（Stripe 側の Detach は backend で非同期に実行される）。 */
export async function deleteCard(
  paymentMethodId: string,
): Promise<CardActionState> {
  const session = await requireValidSession();
  try {
    const client = createGrpcClient(DeleteCardService, session.accessToken);
    await client.deleteCard({ paymentMethodId });
  } catch (e) {
    redirectIfUnauthenticated(e);
    const parsed = parseConnectError(e);
    if (parsed?.businessError) return { error: parsed.businessError };
    return { error: "カードの削除に失敗しました" };
  }
  revalidatePath("/profile/payment-methods");
  return null;
}

/** デフォルトカードを変更する。 */
export async function changeDefaultCard(
  paymentMethodId: string,
): Promise<CardActionState> {
  const session = await requireValidSession();
  try {
    const client = createGrpcClient(
      ChangeDefaultCardService,
      session.accessToken,
    );
    await client.changeDefaultCard({ paymentMethodId });
  } catch (e) {
    redirectIfUnauthenticated(e);
    const parsed = parseConnectError(e);
    if (parsed?.businessError) return { error: parsed.businessError };
    return { error: "デフォルトカードの変更に失敗しました" };
  }
  revalidatePath("/profile/payment-methods");
  return null;
}
