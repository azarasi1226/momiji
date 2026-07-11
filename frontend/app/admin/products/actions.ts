"use server";

import { revalidatePath } from "next/cache";
import { redirect } from "next/navigation";
import { ulid } from "ulid";
import { IssueImageUploadUrlService } from "@/grpc/gen/momiji/image/issueuploadurl/issueuploadurl_pb.js";
import { CreateProductService } from "@/grpc/gen/momiji/product/create/create_pb.js";
import { DiscontinueProductService } from "@/grpc/gen/momiji/product/discontinue/discontinue_pb.js";
import { UpdateProductService } from "@/grpc/gen/momiji/product/update/update_pb.js";
import { AdjustStockService } from "@/grpc/gen/momiji/stock/adjust/adjust_pb.js";
import { StockAdjustmentReason } from "@/grpc/gen/momiji/stock/reason_pb.js";
import { ReceiveStockService } from "@/grpc/gen/momiji/stock/receive/receive_pb.js";
import { toActionError, toSimpleActionError } from "@/lib/action-utils";
import { createGrpcClient } from "@/lib/grpc";
import { redirectIfUnauthenticated } from "@/lib/grpc-error";
import { requireValidSession } from "@/lib/session";

export type {
  ListProductsParams,
  Product,
  ProductListItem,
  ProductsPage,
  Stock,
} from "./queries";

export type ProductFormState = {
  success?: boolean;
  error?: string;
  fieldErrors?: Record<string, string>;
} | null;

export type ProductActionState = { error?: string } | null;

export async function createProduct(
  _prevState: ProductFormState,
  formData: FormData,
): Promise<ProductFormState> {
  const session = await requireValidSession();
  const brandId = (formData.get("brandId") as string) ?? "";
  const name = (formData.get("name") as string) ?? "";
  const description = (formData.get("description") as string) ?? "";
  const imageUrl = (formData.get("imageUrl") as string) ?? "";
  const price = Number(formData.get("price") ?? 0);

  try {
    const client = createGrpcClient(CreateProductService, session.accessToken);
    await client.createProduct({
      id: ulid(),
      brandId,
      name,
      description,
      imageUrl,
      price,
    });
  } catch (e) {
    redirectIfUnauthenticated(e);
    return toActionError(e);
  }

  revalidatePath("/admin/products");
  redirect("/admin/products");
}

export async function updateProduct(
  _prevState: ProductFormState,
  formData: FormData,
): Promise<ProductFormState> {
  const session = await requireValidSession();
  const id = formData.get("id") as string;
  const name = (formData.get("name") as string) ?? "";
  const description = (formData.get("description") as string) ?? "";
  const imageUrl = (formData.get("imageUrl") as string) ?? "";
  const price = Number(formData.get("price") ?? 0);

  try {
    const client = createGrpcClient(UpdateProductService, session.accessToken);
    await client.updateProduct({ id, name, description, imageUrl, price });
  } catch (e) {
    redirectIfUnauthenticated(e);
    return toActionError(e);
  }

  revalidatePath("/admin/products");
  revalidatePath(`/admin/products/${id}`);
  return { success: true };
}

export async function discontinueProduct(
  id: string,
): Promise<ProductActionState> {
  const session = await requireValidSession();
  try {
    const client = createGrpcClient(
      DiscontinueProductService,
      session.accessToken,
    );
    await client.discontinueProduct({ id });
  } catch (e) {
    redirectIfUnauthenticated(e);
    return toSimpleActionError(e);
  }

  revalidatePath("/admin/products");
  redirect("/admin/products");
}

const ADJUST_REASON_MAP: Record<string, StockAdjustmentReason> = {
  DAMAGED: StockAdjustmentReason.DAMAGED,
  LOST: StockAdjustmentReason.LOST,
  STOCKTAKING: StockAdjustmentReason.STOCKTAKING,
  CORRECTION: StockAdjustmentReason.CORRECTION,
  OTHER: StockAdjustmentReason.OTHER,
};

export async function receiveStock(
  _prevState: ProductFormState,
  formData: FormData,
): Promise<ProductFormState> {
  const session = await requireValidSession();
  const productId = formData.get("productId") as string;
  const quantity = Number(formData.get("quantity") ?? 0);

  try {
    const client = createGrpcClient(ReceiveStockService, session.accessToken);
    await client.receiveStock({ productId, quantity });
  } catch (e) {
    redirectIfUnauthenticated(e);
    return toActionError(e);
  }

  revalidatePath(`/admin/products/${productId}`);
  return { success: true };
}

export async function adjustStock(
  _prevState: ProductFormState,
  formData: FormData,
): Promise<ProductFormState> {
  const session = await requireValidSession();
  const productId = formData.get("productId") as string;
  const quantity = Number(formData.get("quantity") ?? 0);
  const reason =
    ADJUST_REASON_MAP[(formData.get("reason") as string) ?? ""] ??
    StockAdjustmentReason.UNSPECIFIED;

  try {
    const client = createGrpcClient(AdjustStockService, session.accessToken);
    await client.adjustStock({ productId, quantity, reason });
  } catch (e) {
    redirectIfUnauthenticated(e);
    return toActionError(e);
  }

  revalidatePath(`/admin/products/${productId}`);
  return { success: true };
}

export async function issueImageUploadUrl(
  contentType: string,
): Promise<{ uploadUrl: string; publicUrl: string } | { error: string }> {
  const session = await requireValidSession();
  try {
    const client = createGrpcClient(
      IssueImageUploadUrlService,
      session.accessToken,
    );
    const res = await client.issueImageUploadUrl({ contentType });
    return { uploadUrl: res.uploadUrl, publicUrl: res.publicUrl };
  } catch (e) {
    redirectIfUnauthenticated(e);
    return toSimpleActionError(e);
  }
}
