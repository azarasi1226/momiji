"use client";

import { useActionState } from "react";
import {
  type Brand,
  type BrandFormState,
  updateBrand,
} from "@/app/admin/brands/actions";
import { FieldError } from "@/components/form/field-error";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";

export function BrandEditForm({ brand }: { brand: Brand }) {
  const [state, formAction, isPending] = useActionState<
    BrandFormState,
    FormData
  >(updateBrand, null);
  const fieldErrors = state?.fieldErrors;

  return (
    <form action={formAction} className="flex w-full flex-col gap-4">
      <input type="hidden" name="id" value={brand.id} />

      <div className="flex flex-col gap-1.5">
        <Label htmlFor="name">ブランド名</Label>
        <Input
          id="name"
          name="name"
          type="text"
          defaultValue={brand.name}
          required
          aria-invalid={!!fieldErrors?.name}
        />
        <FieldError message={fieldErrors?.name} />
      </div>

      <div className="flex flex-col gap-1.5">
        <Label htmlFor="description">説明（任意）</Label>
        <Textarea
          id="description"
          name="description"
          rows={5}
          defaultValue={brand.description}
          aria-invalid={!!fieldErrors?.description}
        />
        <FieldError message={fieldErrors?.description} />
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
