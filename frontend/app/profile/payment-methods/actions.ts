"use server";

import { revalidatePath } from "next/cache";
import { ChangeDefaultCardService } from "@/grpc/gen/momiji/payment/changedefaultcard/changedefault_pb.js";
import { DeleteCardService } from "@/grpc/gen/momiji/payment/deletecard/delete_pb.js";
import { PrepareCardRegistrationService } from "@/grpc/gen/momiji/payment/preparecard/prepare_pb.js";
import { toSimpleActionError } from "@/lib/action-utils";
import { createGrpcClient } from "@/lib/grpc";
import { redirectIfUnauthenticated } from "@/lib/grpc-error";
import { requireValidSession } from "@/lib/session";

export type { Card } from "./queries";

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
    return toSimpleActionError(e);
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
    return toSimpleActionError(e);
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
    return toSimpleActionError(e);
  }
  revalidatePath("/profile/payment-methods");
  return null;
}
