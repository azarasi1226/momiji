"use client";

import { useActionState } from "react";
import { FieldError } from "@/components/form/field-error";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  type UpdateProfileState,
  type UserProfile,
  updateProfile,
} from "./actions";

// プロフィールは email（変更フロー別建て）と name のみ。 住所・電話は配送先ページで管理する。
export function ProfileForm({ profile }: { profile: UserProfile }) {
  const [state, formAction, isPending] = useActionState<
    UpdateProfileState,
    FormData
  >(updateProfile, null);
  const fieldErrors = state?.fieldErrors;

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
