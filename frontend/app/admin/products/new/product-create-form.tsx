"use client"

import { useActionState } from "react"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Textarea } from "@/components/ui/textarea"
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"
import { ImageUploadField } from "../image-upload-field"
import { createProduct, type ProductFormState } from "../actions"
import { FieldError } from "@/components/form/field-error"

export function ProductCreateForm({
  brands,
}: {
  brands: { id: string; name: string }[]
}) {
  const [state, formAction, isPending] = useActionState<ProductFormState, FormData>(
    createProduct,
    null,
  )
  const fieldErrors = state?.fieldErrors

  return (
    <form action={formAction} className="flex w-full flex-col gap-4">
      <div className="flex flex-col gap-1.5">
        <Label htmlFor="brandId">ブランド</Label>
        {brands.length === 0 ? (
          <p className="text-sm text-destructive">
            紐づけられる ACTIVE なブランドがありません。 先にブランドを作成してください。
          </p>
        ) : (
          <Select name="brandId" required>
            <SelectTrigger id="brandId" aria-invalid={!!fieldErrors?.id}>
              <SelectValue placeholder="選択してください" />
            </SelectTrigger>
            <SelectContent>
              {brands.map((brand) => (
                <SelectItem key={brand.id} value={brand.id}>
                  {brand.name}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        )}
        <FieldError message={fieldErrors?.id} />
      </div>

      <div className="flex flex-col gap-1.5">
        <Label htmlFor="name">商品名</Label>
        <Input id="name" name="name" type="text" required aria-invalid={!!fieldErrors?.name} />
        <FieldError message={fieldErrors?.name} />
      </div>

      <div className="flex flex-col gap-1.5">
        <Label htmlFor="description">商品説明</Label>
        <Textarea
          id="description"
          name="description"
          rows={5}
          required
          aria-invalid={!!fieldErrors?.description}
        />
        <FieldError message={fieldErrors?.description} />
      </div>

      <div className="flex flex-col gap-1.5">
        <Label>商品画像（任意）</Label>
        <ImageUploadField name="imageUrl" />
        <FieldError message={fieldErrors?.imageUrl} />
      </div>

      <div className="flex flex-col gap-1.5">
        <Label htmlFor="price">価格（円）</Label>
        <Input
          id="price"
          name="price"
          type="number"
          min={1}
          required
          aria-invalid={!!fieldErrors?.price}
        />
        <FieldError message={fieldErrors?.price} />
      </div>

      {state?.error && <p className="text-sm text-destructive">{state.error}</p>}

      <Button type="submit" disabled={isPending || brands.length === 0} className="mt-2 w-fit">
        {isPending ? "作成中..." : "作成"}
      </Button>
    </form>
  )
}
