"use client";

import { useActionState } from "react";
import { FieldError } from "@/components/form/field-error";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { type Product, type ProductFormState, updateProduct } from "../actions";
import { ImageUploadField } from "../image-upload-field";

export function ProductEditForm({
  product,
  brandName,
}: {
  product: Product;
  brandName: string;
}) {
  const [state, formAction, isPending] = useActionState<
    ProductFormState,
    FormData
  >(updateProduct, null);
  const fieldErrors = state?.fieldErrors;

  return (
    <form action={formAction} className="flex w-full flex-col gap-4">
      <input type="hidden" name="id" value={product.id} />

      {/* ブランドは付け替え不可（UpdateProduct に brandId は無い）。 参照のみ表示。 */}
      <div className="flex flex-col gap-1.5">
        <Label>ブランド</Label>
        <p className="rounded-lg border bg-muted/40 px-4 py-2 text-sm text-muted-foreground">
          {brandName}
        </p>
      </div>

      <div className="flex flex-col gap-1.5">
        <Label htmlFor="name">商品名</Label>
        <Input
          id="name"
          name="name"
          type="text"
          defaultValue={product.name}
          required
          aria-invalid={!!fieldErrors?.name}
        />
        <FieldError message={fieldErrors?.name} />
      </div>

      <div className="flex flex-col gap-1.5">
        <Label htmlFor="description">商品説明</Label>
        <Textarea
          id="description"
          name="description"
          rows={5}
          defaultValue={product.description}
          required
          aria-invalid={!!fieldErrors?.description}
        />
        <FieldError message={fieldErrors?.description} />
      </div>

      <div className="flex flex-col gap-1.5">
        <Label>商品画像（任意）</Label>
        <ImageUploadField name="imageUrl" defaultUrl={product.imageUrl} />
        <FieldError message={fieldErrors?.imageUrl} />
      </div>

      <div className="flex flex-col gap-1.5">
        <Label htmlFor="price">価格（円）</Label>
        <Input
          id="price"
          name="price"
          type="number"
          min={1}
          defaultValue={product.price}
          required
          aria-invalid={!!fieldErrors?.price}
        />
        <FieldError message={fieldErrors?.price} />
      </div>

      {state?.error && (
        <p className="text-sm text-destructive">{state.error}</p>
      )}
      {state?.success && <p className="text-sm text-green-600">更新しました</p>}

      <Button type="submit" disabled={isPending} className="mt-2 w-fit">
        {isPending ? "更新中..." : "更新"}
      </Button>
    </form>
  );
}
