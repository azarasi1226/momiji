"use server";

import { revalidatePath } from "next/cache";
import { signOut } from "@/auth";
import { ConfirmEmailChangeService } from "@/grpc/gen/momiji/user/changeemail/confirm/confirm_pb.js";
import { RequestEmailChangeService } from "@/grpc/gen/momiji/user/changeemail/request/request_pb.js";
import { DeleteUserService } from "@/grpc/gen/momiji/user/delete/delete_pb.js";
import { UpdateUserService } from "@/grpc/gen/momiji/user/update/update_pb.js";
import { createGrpcClient } from "@/lib/grpc";
import { parseConnectError, redirectIfUnauthenticated } from "@/lib/grpc-error";
import { requireValidSession } from "@/lib/session";

export type UpdateProfileState = {
  success?: boolean;
  error?: string;
  fieldErrors?: Record<string, string>;
} | null;

export async function updateProfile(
  _prevState: UpdateProfileState,
  formData: FormData,
): Promise<UpdateProfileState> {
  const session = await requireValidSession();

  try {
    const client = createGrpcClient(UpdateUserService, session.accessToken);
    await client.updateUser({
      name: formData.get("name") as string,
    });
  } catch (e) {
    redirectIfUnauthenticated(e);
    const parsed = parseConnectError(e);
    if (parsed?.fieldErrors) return { fieldErrors: parsed.fieldErrors };
    if (parsed?.businessError) return { error: parsed.businessError };
    if (parsed?.unknownError) {
      return {
        error: `${parsed.unknownError.message} (問い合わせ番号: ${parsed.unknownError.correlationId})`,
      };
    }
    if (parsed?.fallback) return { error: parsed.fallback };
    return { error: "ユーザー情報の更新に失敗しました" };
  }

  revalidatePath("/profile");
  return { success: true };
}

export type DeleteAccountState = {
  error?: string;
} | null;

export async function deleteAccount(): Promise<DeleteAccountState> {
  const session = await requireValidSession();

  try {
    const client = createGrpcClient(DeleteUserService, session.accessToken);
    await client.deleteUser({});
  } catch (e) {
    redirectIfUnauthenticated(e);
    const parsed = parseConnectError(e);
    if (parsed?.businessError) return { error: parsed.businessError };
    if (parsed?.unknownError) {
      return {
        error: `${parsed.unknownError.message} (問い合わせ番号: ${parsed.unknownError.correlationId})`,
      };
    }
    if (parsed?.fallback) return { error: parsed.fallback };
    return { error: "アカウントの削除に失敗しました" };
  }

  await signOut({ redirectTo: "/" });
  return null;
}

export type EmailChangeState = {
  success?: boolean;
  error?: string;
  fieldErrors?: Record<string, string>;
} | null;

export async function requestEmailChange(
  _prevState: EmailChangeState,
  formData: FormData,
): Promise<EmailChangeState> {
  const session = await requireValidSession();

  try {
    const client = createGrpcClient(
      RequestEmailChangeService,
      session.accessToken,
    );
    await client.requestEmailChange({
      newEmail: formData.get("newEmail") as string,
    });
  } catch (e) {
    redirectIfUnauthenticated(e);
    const parsed = parseConnectError(e);
    if (parsed?.fieldErrors) return { fieldErrors: parsed.fieldErrors };
    if (parsed?.businessError) return { error: parsed.businessError };
    if (parsed?.unknownError) {
      return {
        error: `${parsed.unknownError.message} (問い合わせ番号: ${parsed.unknownError.correlationId})`,
      };
    }
    if (parsed?.fallback) return { error: parsed.fallback };
    return { error: "メールアドレス変更リクエストに失敗しました" };
  }

  return { success: true };
}

export async function confirmEmailChange(
  _prevState: EmailChangeState,
  formData: FormData,
): Promise<EmailChangeState> {
  const session = await requireValidSession();

  try {
    const client = createGrpcClient(
      ConfirmEmailChangeService,
      session.accessToken,
    );
    await client.confirmEmailChange({
      token: formData.get("token") as string,
    });
  } catch (e) {
    redirectIfUnauthenticated(e);
    const parsed = parseConnectError(e);
    if (parsed?.fieldErrors) return { fieldErrors: parsed.fieldErrors };
    if (parsed?.businessError) return { error: parsed.businessError };
    if (parsed?.unknownError) {
      return {
        error: `${parsed.unknownError.message} (問い合わせ番号: ${parsed.unknownError.correlationId})`,
      };
    }
    if (parsed?.fallback) return { error: parsed.fallback };
    return { error: "メールアドレスの変更確認に失敗しました" };
  }

  revalidatePath("/profile");
  return { success: true };
}
