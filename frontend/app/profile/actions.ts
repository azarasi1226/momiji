"use server";

import { revalidatePath } from "next/cache";
import { signOut } from "@/auth";
import { toActionError, toSimpleActionError } from "@/lib/action-utils";
import { ConfirmEmailChangeService } from "@/grpc/gen/momiji/user/changeemail/confirm/confirm_pb.js";
import { RequestEmailChangeService } from "@/grpc/gen/momiji/user/changeemail/request/request_pb.js";
import { DeleteUserService } from "@/grpc/gen/momiji/user/delete/delete_pb.js";
import { UpdateUserService } from "@/grpc/gen/momiji/user/update/update_pb.js";
import { createGrpcClient } from "@/lib/grpc";
import { redirectIfUnauthenticated } from "@/lib/grpc-error";
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
    return toActionError(e);
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
    return toSimpleActionError(e);
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
    return toActionError(e);
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
    return toActionError(e);
  }

  revalidatePath("/profile");
  return { success: true };
}
