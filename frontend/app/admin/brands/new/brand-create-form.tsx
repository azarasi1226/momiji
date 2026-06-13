"use client"

import { useActionState } from "react"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Textarea } from "@/components/ui/textarea"
import { createBrand, type BrandFormState } from "../actions"
import { FieldError } from "@/components/form/field-error"

export function BrandCreateForm() {
  const [state, formAction, isPending] = useActionState<BrandFormState, FormData>(
    createBrand,
    null,
  )
  const fieldErrors = state?.fieldErrors

  return (
    <form action={formAction} className="flex w-full flex-col gap-4">
      <div className="flex flex-col gap-1.5">
        <Label htmlFor="name">ブランド名</Label>
        <Input id="name" name="name" type="text" required aria-invalid={!!fieldErrors?.name} />
        <FieldError message={fieldErrors?.name} />
      </div>

      <div className="flex flex-col gap-1.5">
        <Label htmlFor="description">説明（任意）</Label>
        <Textarea
          id="description"
          name="description"
          rows={5}
          aria-invalid={!!fieldErrors?.description}
        />
        <FieldError message={fieldErrors?.description} />
      </div>

      {state?.error && <p className="text-sm text-destructive">{state.error}</p>}

      <Button type="submit" disabled={isPending} className="mt-2 w-fit">
        {isPending ? "作成中..." : "作成"}
      </Button>
    </form>
  )
}
