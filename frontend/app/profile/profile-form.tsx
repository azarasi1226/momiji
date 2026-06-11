"use client"

import { useActionState } from "react"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { PhoneNumberFields, PostalCodeFields } from "@/components/form/digit-fields"
import { updateProfile, type UpdateProfileState, type UserProfile } from "./actions"

function FieldError({ message }: { message?: string }) {
  if (!message) return null
  return <p className="text-xs text-destructive">{message}</p>
}

export function ProfileForm({ profile }: { profile: UserProfile }) {
  const [state, formAction, isPending] = useActionState<UpdateProfileState, FormData>(
    updateProfile,
    null,
  )
  const fieldErrors = state?.fieldErrors

  return (
    <form action={formAction} className="flex w-full flex-col gap-4">
      <div className="flex flex-col gap-1.5">
        <Label htmlFor="email">メールアドレス</Label>
        <Input id="email" type="email" value={profile.email} disabled />
      </div>

      <div className="flex flex-col gap-1.5">
        <Label htmlFor="name">名前</Label>
        <Input
          id="name"
          name="name"
          type="text"
          defaultValue={profile.name}
          required
          aria-invalid={!!fieldErrors?.name}
        />
        <FieldError message={fieldErrors?.name} />
      </div>

      <div className="flex flex-col gap-1.5">
        <Label htmlFor="phoneNumber1">電話番号</Label>
        <PhoneNumberFields defaultValue={profile.phoneNumber} invalid={!!fieldErrors?.phoneNumber} />
        <FieldError message={fieldErrors?.phoneNumber} />
      </div>

      <div className="flex flex-col gap-1.5">
        <Label htmlFor="postalCode1">郵便番号</Label>
        <PostalCodeFields defaultValue={profile.postalCode} invalid={!!fieldErrors?.postalCode} />
        <FieldError message={fieldErrors?.postalCode} />
      </div>

      <div className="flex flex-col gap-1.5">
        <Label htmlFor="address1">住所1</Label>
        <Input
          id="address1"
          name="address1"
          type="text"
          defaultValue={profile.address1}
          required
          aria-invalid={!!fieldErrors?.address1}
        />
        <FieldError message={fieldErrors?.address1} />
      </div>

      <div className="flex flex-col gap-1.5">
        <Label htmlFor="address2">住所2</Label>
        <Input
          id="address2"
          name="address2"
          type="text"
          defaultValue={profile.address2}
          required
          aria-invalid={!!fieldErrors?.address2}
        />
        <FieldError message={fieldErrors?.address2} />
      </div>

      {state?.error && <p className="text-sm text-destructive">{state.error}</p>}
      {state?.success && <p className="text-sm text-green-600">更新しました</p>}

      <Button type="submit" disabled={isPending} className="mt-2 w-fit">
        {isPending ? "更新中..." : "更新"}
      </Button>
    </form>
  )
}
