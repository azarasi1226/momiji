"use server";

import { revalidatePath } from "next/cache";
import { redirect } from "next/navigation";
import { ulid } from "ulid";
import { ArchiveBrandService } from "@/grpc/gen/momiji/brand/archive/archive_pb.js";
import { CreateBrandService } from "@/grpc/gen/momiji/brand/create/create_pb.js";
import { UpdateBrandService } from "@/grpc/gen/momiji/brand/update/update_pb.js";
import { toActionError, toSimpleActionError } from "@/lib/action-utils";
import { createGrpcClient } from "@/lib/grpc";
import { redirectIfUnauthenticated } from "@/lib/grpc-error";
import { requireValidSession } from "@/lib/session";

export type { Brand } from "./queries";

export type BrandFormState = {
  success?: boolean;
  error?: string;
  fieldErrors?: Record<string, string>;
} | null;

export type BrandActionState = { error?: string } | null;

export async function createBrand(
  _prevState: BrandFormState,
  formData: FormData,
): Promise<BrandFormState> {
  const session = await requireValidSession();
  const name = (formData.get("name") as string) ?? "";
  const description = (formData.get("description") as string) ?? "";

  try {
    const client = createGrpcClient(CreateBrandService, session.accessToken);
    await client.createBrand({ id: ulid(), name, description });
  } catch (e) {
    redirectIfUnauthenticated(e);
    return toActionError(e);
  }

  revalidatePath("/admin/brands");
  redirect("/admin/brands");
}

export async function updateBrand(
  _prevState: BrandFormState,
  formData: FormData,
): Promise<BrandFormState> {
  const session = await requireValidSession();
  const id = formData.get("id") as string;
  const name = (formData.get("name") as string) ?? "";
  const description = (formData.get("description") as string) ?? "";

  try {
    const client = createGrpcClient(UpdateBrandService, session.accessToken);
    await client.updateBrand({ id, name, description });
  } catch (e) {
    redirectIfUnauthenticated(e);
    return toActionError(e);
  }

  revalidatePath("/admin/brands");
  revalidatePath(`/admin/brands/${id}`);
  return { success: true };
}

export async function archiveBrand(id: string): Promise<BrandActionState> {
  const session = await requireValidSession();
  try {
    const client = createGrpcClient(ArchiveBrandService, session.accessToken);
    await client.archiveBrand({ id });
  } catch (e) {
    redirectIfUnauthenticated(e);
    return toSimpleActionError(e);
  }

  revalidatePath("/admin/brands");
  redirect("/admin/brands");
}
