"use client"

import { useActionState, useState } from "react"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import {
  requestEmailChange,
  confirmEmailChange,
  type EmailChangeState,
} from "./actions"

function FieldError({ message }: { message?: string }) {
  if (!message) return null
  return <p className="text-xs text-destructive">{message}</p>
}

export function EmailChangeForm({ currentEmail }: { currentEmail: string }) {
  const [step, setStep] = useState<"request" | "confirm">("request")

  const [requestState, requestAction, isRequesting] = useActionState<EmailChangeState, FormData>(
    async (prevState, formData) => {
      const result = await requestEmailChange(prevState, formData)
      if (result?.success) {
        setStep("confirm")
      }
      return result
    },
    null,
  )

  const [confirmState, confirmAction, isConfirming] = useActionState<EmailChangeState, FormData>(
    confirmEmailChange,
    null,
  )

  if (step === "confirm") {
    const tokenError = confirmState?.fieldErrors?.token
    return (
      <div className="flex w-full flex-col gap-4">
        <h2 className="text-lg font-medium">メールアドレス変更の確認</h2>
        <p className="text-sm text-muted-foreground">
          新しいメールアドレスに確認メールを送信しました。メールに記載されたトークンを入力してください。
        </p>

        <form action={confirmAction} className="flex w-full flex-col gap-4">
          <div className="flex flex-col gap-1.5">
            <Label htmlFor="token">確認トークン</Label>
            <Input id="token" name="token" type="text" required aria-invalid={!!tokenError} />
            <FieldError message={tokenError} />
          </div>

          {confirmState?.error && <p className="text-sm text-destructive">{confirmState.error}</p>}
          {confirmState?.success && (
            <p className="text-sm text-green-600">メールアドレスを変更しました</p>
          )}

          <div className="flex gap-4">
            <Button type="submit" disabled={isConfirming}>
              {isConfirming ? "確認中..." : "確認"}
            </Button>
            <Button type="button" variant="outline" onClick={() => setStep("request")}>
              戻る
            </Button>
          </div>
        </form>
      </div>
    )
  }

  const newEmailError = requestState?.fieldErrors?.email

  return (
    <div className="flex w-full flex-col gap-4">
      <h2 className="text-lg font-medium">メールアドレスの変更</h2>
      <p className="text-sm text-muted-foreground">現在: {currentEmail}</p>

      <form action={requestAction} className="flex w-full flex-col gap-4">
        <div className="flex flex-col gap-1.5">
          <Label htmlFor="newEmail">新しいメールアドレス</Label>
          <Input id="newEmail" name="newEmail" type="email" required aria-invalid={!!newEmailError} />
          <FieldError message={newEmailError} />
        </div>

        {requestState?.error && <p className="text-sm text-destructive">{requestState.error}</p>}

        <Button type="submit" disabled={isRequesting}>
          {isRequesting ? "送信中..." : "変更リクエスト送信"}
        </Button>
      </form>
    </div>
  )
}
