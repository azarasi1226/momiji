"use client"

import { useActionState } from "react"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { updateProfile, type UpdateProfileState, type UserProfile } from "./actions"

function FieldError({ message }: { message?: string }) {
  if (!message) return null
  return <p className="text-xs text-destructive">{message}</p>
}

// 数字以外を弾き、 枠が埋まったら次の枠へ自動フォーカスする（電話番号・郵便番号の分割枠用）。
function digitsAutoAdvance(e: React.FormEvent<HTMLInputElement>, nextId?: string) {
  const input = e.currentTarget
  input.value = input.value.replace(/\D/g, "")
  if (nextId && input.value.length >= input.maxLength) {
    document.getElementById(nextId)?.focus()
  }
}

export function ProfileForm({ profile }: { profile: UserProfile }) {
  const [state, formAction, isPending] = useActionState<UpdateProfileState, FormData>(
    updateProfile,
    null,
  )
  const fieldErrors = state?.fieldErrors

  // 保存形式はハイフン区切り（例: 080-8080-8080 / 123-0002）。 分割枠の初期値に分解する。
  const [phone1 = "", phone2 = "", phone3 = ""] = profile.phoneNumber.split("-")
  const [postal1 = "", postal2 = ""] = profile.postalCode.split("-")

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
        <div className="flex items-center gap-2">
          <Input
            id="phoneNumber1"
            name="phoneNumber1"
            type="tel"
            inputMode="numeric"
            maxLength={4}
            className="w-20 text-center"
            defaultValue={phone1}
            required
            aria-invalid={!!fieldErrors?.phoneNumber}
            aria-label="電話番号（市外局番）"
            onInput={(e) => digitsAutoAdvance(e, "phoneNumber2")}
          />
          <span className="text-muted-foreground">-</span>
          <Input
            id="phoneNumber2"
            name="phoneNumber2"
            type="tel"
            inputMode="numeric"
            maxLength={4}
            className="w-20 text-center"
            defaultValue={phone2}
            required
            aria-invalid={!!fieldErrors?.phoneNumber}
            aria-label="電話番号（市内局番）"
            onInput={(e) => digitsAutoAdvance(e, "phoneNumber3")}
          />
          <span className="text-muted-foreground">-</span>
          <Input
            id="phoneNumber3"
            name="phoneNumber3"
            type="tel"
            inputMode="numeric"
            maxLength={4}
            className="w-20 text-center"
            defaultValue={phone3}
            required
            aria-invalid={!!fieldErrors?.phoneNumber}
            aria-label="電話番号（加入者番号）"
            onInput={(e) => digitsAutoAdvance(e)}
          />
        </div>
        <FieldError message={fieldErrors?.phoneNumber} />
      </div>

      <div className="flex flex-col gap-1.5">
        <Label htmlFor="postalCode1">郵便番号</Label>
        <div className="flex items-center gap-2">
          <Input
            id="postalCode1"
            name="postalCode1"
            type="text"
            inputMode="numeric"
            maxLength={3}
            className="w-16 text-center"
            defaultValue={postal1}
            required
            aria-invalid={!!fieldErrors?.postalCode}
            aria-label="郵便番号（前3桁）"
            onInput={(e) => digitsAutoAdvance(e, "postalCode2")}
          />
          <span className="text-muted-foreground">-</span>
          <Input
            id="postalCode2"
            name="postalCode2"
            type="text"
            inputMode="numeric"
            maxLength={4}
            className="w-20 text-center"
            defaultValue={postal2}
            required
            aria-invalid={!!fieldErrors?.postalCode}
            aria-label="郵便番号（後4桁）"
            onInput={(e) => digitsAutoAdvance(e)}
          />
        </div>
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
